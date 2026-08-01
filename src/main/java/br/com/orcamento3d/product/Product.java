package br.com.orcamento3d.product;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity @Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private User owner;
 @Column(nullable=false,length=140) private String name;
 @Column(length=1000) private String description;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal price=BigDecimal.ZERO;
 @Column(nullable=false,length=80) private String category="Outros";
 @Lob private String imageDataUrl;
 @Lob private String image2DataUrl;
 @Lob private String image3DataUrl;
 @Lob private String imageGallery;
 @Column(length=500) private String colors;
 @Column(length=500) private String sizes;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal technicalCost=BigDecimal.ZERO;
 @Column(nullable=false) private boolean published=true;
 @Column(nullable=false) private boolean featured=false;
 @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
 public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;}
 public String getImageDataUrl(){return imageDataUrl;} public void setImageDataUrl(String v){imageDataUrl=v;} public boolean isPublished(){return published;} public void setPublished(boolean v){published=v;}
 public String getImage2DataUrl(){return image2DataUrl;} public void setImage2DataUrl(String v){image2DataUrl=v;} public String getImage3DataUrl(){return image3DataUrl;} public void setImage3DataUrl(String v){image3DataUrl=v;}
 public List<String> getImages(){
  if(imageGallery!=null&&!imageGallery.isBlank())return Arrays.stream(imageGallery.split("\\n")).filter(v->!v.isBlank()).toList();
  return Arrays.asList(imageDataUrl,image2DataUrl,image3DataUrl).stream().filter(v->v!=null&&!v.isBlank()).toList();
 }
 public void setImages(List<String> values){
  List<String> images=values==null?List.of():values.stream().filter(v->v!=null&&!v.isBlank()).toList();
  imageGallery=images.isEmpty()?null:String.join("\n",images);
  imageDataUrl=images.size()>0?images.get(0):null;image2DataUrl=images.size()>1?images.get(1):null;image3DataUrl=images.size()>2?images.get(2):null;
 }
 public String getColors(){return colors;} public void setColors(String v){colors=v;} public String getSizes(){return sizes;} public void setSizes(String v){sizes=v;}
 public BigDecimal getTechnicalCost(){return technicalCost;} public void setTechnicalCost(BigDecimal v){technicalCost=v;}
 public boolean isFeatured(){return featured;} public void setFeatured(boolean v){featured=v;} public Instant getCreatedAt(){return createdAt;}
}
