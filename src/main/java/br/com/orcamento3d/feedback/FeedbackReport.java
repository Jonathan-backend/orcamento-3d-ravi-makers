package br.com.orcamento3d.feedback;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feedback_reports")
public class FeedbackReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;
    @Column(nullable = false, length = 3000)
    private String description;
    @Column(length = 500)
    private String page;
    @Column(nullable = false, length = 20)
    private String status = "OPEN";
    @Lob @Column(name = "screenshot")
    private byte[] screenshot;
    @Column(length = 40)
    private String screenshotContentType;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId(){return id;}
    public User getOwner(){return owner;}
    public void setOwner(User owner){this.owner=owner;}
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}
    public String getPage(){return page;}
    public void setPage(String page){this.page=page;}
    public String getStatus(){return status;}
    public void setStatus(String status){this.status=status;}
    public byte[] getScreenshot(){return screenshot;}
    public void setScreenshot(byte[] screenshot){this.screenshot=screenshot;}
    public String getScreenshotContentType(){return screenshotContentType;}
    public void setScreenshotContentType(String type){this.screenshotContentType=type;}
    public Instant getCreatedAt(){return createdAt;}
}
