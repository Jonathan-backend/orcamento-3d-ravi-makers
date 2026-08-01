package br.com.orcamento3d.feedback;

import br.com.orcamento3d.user.UserRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private static final long MAX_SCREENSHOT = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/png","image/jpeg","image/webp");
    private final FeedbackRepository reports;
    private final UserRepository users;

    public FeedbackController(FeedbackRepository reports,UserRepository users){
        this.reports=reports;this.users=users;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Response> create(@RequestParam String description,
                                           @RequestParam(required=false) String page,
                                           @RequestPart(required=false) MultipartFile screenshot,
                                           Authentication auth) throws IOException {
        String text=description==null?"":description.trim();
        if(text.length()<10 || text.length()>3000)
            throw new IllegalArgumentException("Descreva o problema com pelo menos 10 e no máximo 3.000 caracteres.");
        FeedbackReport report=new FeedbackReport();
        report.setOwner(users.findByEmailIgnoreCase(auth.getName()).orElseThrow());
        report.setDescription(text);
        report.setPage(page==null?null:page.substring(0,Math.min(500,page.length())));
        if(screenshot!=null && !screenshot.isEmpty()){
            if(screenshot.getSize()>MAX_SCREENSHOT) throw new IllegalArgumentException("A imagem deve ter no máximo 5 MB.");
            String type=screenshot.getContentType();
            if(type==null || !ALLOWED.contains(type)) throw new IllegalArgumentException("Envie uma imagem PNG, JPEG ou WebP.");
            report.setScreenshot(screenshot.getBytes());
            report.setScreenshotContentType(type);
        }
        FeedbackReport saved=reports.save(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(Response.from(saved));
    }

    @GetMapping
    @Transactional(readOnly=true)
    public List<Response> list(Authentication auth){
        return reports.findTop20ByOwnerEmailOrderByCreatedAtDesc(auth.getName()).stream().map(Response::from).toList();
    }

    @GetMapping("/{id}/screenshot")
    @Transactional(readOnly=true)
    public ResponseEntity<byte[]> screenshot(@PathVariable Long id,Authentication auth){
        FeedbackReport report=reports.findByIdAndOwnerEmail(id,auth.getName()).orElseThrow();
        if(report.getScreenshot()==null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(report.getScreenshotContentType())).body(report.getScreenshot());
    }

    public record Response(Long id,String description,String page,String status,boolean hasScreenshot,Instant createdAt){
        static Response from(FeedbackReport r){return new Response(r.getId(),r.getDescription(),r.getPage(),r.getStatus(),r.getScreenshot()!=null,r.getCreatedAt());}
    }
}
