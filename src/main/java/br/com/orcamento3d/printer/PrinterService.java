package br.com.orcamento3d.printer;

import br.com.orcamento3d.printer.PrinterDtos.*;
import br.com.orcamento3d.quote.*;
import br.com.orcamento3d.user.UserRepository;
import br.com.orcamento3d.config.SecretCipher;
import org.springframework.stereotype.Service;
import tools.jackson.databind.*;

import java.net.URI;
import java.net.InetAddress;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class PrinterService {
    private final PrinterRepository printers;
    private final UserRepository users;
    private final QuoteRepository quotes;
    private final ObjectMapper json;
    private final SecretCipher cipher;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public PrinterService(PrinterRepository printers, UserRepository users,
                          QuoteRepository quotes, ObjectMapper json, SecretCipher cipher) {
        this.printers = printers; this.users = users; this.quotes = quotes; this.json = json;
        this.cipher = cipher;
    }

    public List<Response> list(String email) {
        return printers.findByOwnerEmailOrderByName(email).stream()
                .map(p -> Response.from(p, readStatus(p, email))).toList();
    }

    public Response create(Request request, String email) {
        Printer p = new Printer();
        p.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());
        apply(p, request, false);
        p = printers.save(p);
        return Response.from(p, readStatus(p, email));
    }

    public Response update(Long id, Request request, String email) {
        Printer p = owned(id, email);
        apply(p, request, true);
        p = printers.save(p);
        return Response.from(p, readStatus(p, email));
    }

    public void delete(Long id, String email) {
        printers.delete(owned(id, email));
    }

    public Response status(Long id, String email) {
        Printer p = owned(id, email);
        return Response.from(p, readStatus(p, email));
    }

    private Printer owned(Long id, String email) {
        return printers.findByIdAndOwnerEmail(id, email)
                .orElseThrow(() -> new NoSuchElementException("Impressora não encontrada"));
    }

    private void apply(Printer p, Request r, boolean preserveSecret) {
        p.setName(r.name().trim());
        p.setManufacturer(clean(r.manufacturer()));
        p.setModel(clean(r.model()));
        p.setPowerWatts(r.powerWatts());
        p.setAcquisitionCost(r.acquisitionCost() == null ? java.math.BigDecimal.ZERO : r.acquisitionCost());
        p.setUsefulLifeHours(r.usefulLifeHours());
        p.setMaintenancePerHour(r.maintenancePerHour() == null ? java.math.BigDecimal.ZERO : r.maintenancePerHour());
        p.setNotes(clean(r.notes()));
        p.setType(r.type());
        p.setBaseUrl(normalizeUrl(r.baseUrl(), r.type()));
        if (!preserveSecret || (r.apiKey() != null && !r.apiKey().isBlank())) {
            p.setApiKey(cipher.encrypt(clean(r.apiKey())));
        }
        p.setMonitoringEnabled(r.monitoringEnabled() && r.type() != PrinterType.MANUAL);
        p.setActive(r.active());
    }

    private String normalizeUrl(String value, PrinterType type) {
        if (type == PrinterType.MANUAL || value == null || value.isBlank()) return null;
        URI uri;
        try { uri = URI.create(value.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Endereço de rede inválido"); }
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Use um endereço HTTP ou HTTPS válido, sem credenciais na URL");
        }
        validatePrinterHost(uri);
        return value.trim().replaceAll("/+$", "");
    }

    private void validatePrinterHost(URI uri) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(address -> !isPrivateLan(address))) {
                throw new IllegalArgumentException(
                        "Por segurança, o monitoramento aceita somente endereços da rede local da impressora");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível validar o endereço de rede da impressora");
        }
    }

    private boolean isPrivateLan(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 10 || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168);
        }
        int first = bytes[0] & 0xff;
        return first == 0xfc || first == 0xfd;
    }

    private Status readStatus(Printer p, String email) {
        if (!p.isMonitoringEnabled() || p.getBaseUrl() == null) return Status.manual();
        try {
            Status raw = switch (p.getType()) {
                case MOONRAKER -> moonraker(p);
                case OCTOPRINT -> octoprint(p);
                default -> new Status("UNSUPPORTED", "Conector pendente", null, null, null,
                        "O cadastro funciona, mas este protocolo ainda não possui monitor automático");
            };
            return relate(raw, email);
        } catch (Exception e) {
            return new Status("OFFLINE", "Offline", null, null, null,
                    "Não foi possível consultar a impressora na rede");
        }
    }

    private Status moonraker(Printer p) throws Exception {
        String url = p.getBaseUrl() + "/printer/objects/query?print_stats";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5)).GET();
        if (p.getApiKey() != null) builder.header("X-Api-Key", cipher.decrypt(p.getApiKey()));
        JsonNode root = send(builder.build());
        JsonNode stats = root.path("result").path("status").path("print_stats");
        String state = stats.path("state").asText("unknown");
        String file = stats.path("filename").asText(null);
        return switch (state.toLowerCase()) {
            case "printing" -> new Status("PRINTING", "Imprimindo", file, null, null, null);
            case "paused" -> new Status("PAUSED", "Pausada", file, null, null, null);
            case "error" -> new Status("ERROR", "Erro", file, null, null, stats.path("message").asText());
            case "complete" -> new Status("COMPLETE", "Concluída", file, 100.0, 0L, null);
            case "standby" -> new Status("IDLE", "Disponível", null, 0.0, null, null);
            default -> new Status("ONLINE", "Conectada", file, null, null, state);
        };
    }

    private Status octoprint(Printer p) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(p.getBaseUrl() + "/api/job"))
                .timeout(Duration.ofSeconds(5)).GET();
        if (p.getApiKey() != null) builder.header("X-Api-Key", cipher.decrypt(p.getApiKey()));
        JsonNode root = send(builder.build());
        String state = root.path("state").asText("unknown");
        String file = root.path("job").path("file").path("name").asText(null);
        Double progress = root.path("progress").path("completion").isNumber()
                ? root.path("progress").path("completion").asDouble() : null;
        Long remaining = root.path("progress").path("printTimeLeft").isNumber()
                ? root.path("progress").path("printTimeLeft").asLong() : null;
        String code = state.toLowerCase().contains("printing") ? "PRINTING"
                : state.toLowerCase().contains("paused") ? "PAUSED"
                : state.toLowerCase().contains("operational") ? "IDLE"
                : state.toLowerCase().contains("error") ? "ERROR" : "ONLINE";
        String label = switch (code) {
            case "PRINTING" -> "Imprimindo"; case "PAUSED" -> "Pausada";
            case "IDLE" -> "Disponível"; case "ERROR" -> "Erro"; default -> "Conectada";
        };
        return new Status(code, label, file, progress, remaining, null);
    }

    private JsonNode send(HttpRequest request) throws Exception {
        validatePrinterHost(request.uri());
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Resposta HTTP " + response.statusCode());
        }
        return json.readTree(response.body());
    }

    private Status relate(Status status, String email) {
        if (!"PRINTING".equals(status.code()) || status.fileName() == null) return status;
        String current = basename(status.fileName()).toLowerCase();
        Optional<Quote> match = quotes.findByOwnerEmailOrderByCreatedAtDesc(email).stream()
                .filter(q -> current.equals(q.getFileName().toLowerCase())
                        || current.contains("orcamento-" + q.getId() + "-")).findFirst();
        if (match.isEmpty()) return status;
        Quote q = match.get();
        return new Status(status.code(), "Imprimindo orçamento #" + q.getId(),
                status.fileName(), status.progress(), status.secondsRemaining(),
                q.getFileName());
    }

    private String basename(String value) {
        String clean = value.replace('\\', '/');
        return clean.substring(clean.lastIndexOf('/') + 1);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
