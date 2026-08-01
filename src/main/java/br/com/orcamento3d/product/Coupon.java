package br.com.orcamento3d.product;
import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="coupons",uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","code"}))
public class Coupon{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private User owner;
 @Column(nullable=false,length=30) private String code;
 @Column(nullable=false,precision=7,scale=2) private BigDecimal discountPercent;
 @Column(nullable=false) private boolean active=true;
 public Long getId(){return id;}public User getOwner(){return owner;}public void setOwner(User v){owner=v;}public String getCode(){return code;}public void setCode(String v){code=v;}
 public BigDecimal getDiscountPercent(){return discountPercent;}public void setDiscountPercent(BigDecimal v){discountPercent=v;}public boolean isActive(){return active;}public void setActive(boolean v){active=v;}
}
