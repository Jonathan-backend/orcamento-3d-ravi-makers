package br.com.orcamento3d.feedback;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/support/feedback")
public class SupportFeedbackController {
    private static final Set<String> STATUSES = Set.of("OPEN","ANALYZING","RESOLVED");
    private final FeedbackRepository reports;
    private final SupportAccess access;

    public SupportFeedbackController(FeedbackRepository reports,SupportAccess access) {
        this.reports=reports;this.access=access;
    }

    @GetMapping("/access")
    public Map<String,Boolean> access(Authentication auth) {
        return Map.of("allowed",access.allowed(auth));
    }

    @GetMapping
    @Transactional(readOnly=true)
    public List<Item> list(Authentication auth) {
        access.require(auth);
        return reports.findTop200ByOrderByCreatedAtDesc().stream().map(Item::from).toList();
    }

    @GetMapping("/{id}/screenshot")
    @Transactional(readOnly=true)
    public ResponseEntity<byte[]> screenshot(@PathVariable Long id,Authentication auth) {
        access.require(auth);
        FeedbackReport report=reports.findById(id).orElseThrow();
        if(report.getScreenshot()==null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(report.getScreenshotContentType())).body(report.getScreenshot());
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public Item status(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth) {
        access.require(auth);
        String status=String.valueOf(body.get("status")).toUpperCase();
        if(!STATUSES.contains(status)) throw new IllegalArgumentException("Status inválido.");
        FeedbackReport report=reports.findById(id).orElseThrow();
        report.setStatus(status);
        return Item.from(reports.save(report));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id,Authentication auth) {
        access.require(auth);
        reports.delete(reports.findById(id).orElseThrow());
    }

    public record Item(Long id,String reporterName,String reporterEmail,String description,String page,
                       String status,boolean hasScreenshot,Instant createdAt) {
        static Item from(FeedbackReport report) {
            return new Item(report.getId(),report.getOwner().getName(),report.getOwner().getEmail(),
                    report.getDescription(),report.getPage(),report.getStatus(),report.getScreenshot()!=null,
                    report.getCreatedAt());
        }
    }
}
