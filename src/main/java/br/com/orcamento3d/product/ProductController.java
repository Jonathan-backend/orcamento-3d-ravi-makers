package br.com.orcamento3d.product;

import br.com.orcamento3d.user.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
public class ProductController {
 private static final int MAX_PRODUCT_IMAGE_BYTES=10_000_000;
 private final ProductRepository products;private final UserRepository users;private final CompanyProfileRepository companies;private final CouponRepository coupons;
 public ProductController(ProductRepository p,UserRepository u,CompanyProfileRepository c,CouponRepository cp){products=p;users=u;companies=c;coupons=cp;}
 public record Request(@NotBlank @Size(max=140) String name,@Size(max=1000) String description,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal price,
  @NotBlank @Size(max=80) String category,String imageDataUrl,String image2DataUrl,String image3DataUrl,
  @Size(max=500) String colors,@Size(max=500) String sizes,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") BigDecimal technicalCost,boolean published,boolean featured){}
 public record Response(Long id,String name,String description,BigDecimal price,String category,String imageDataUrl,String image2DataUrl,String image3DataUrl,
  String colors,String sizes,BigDecimal technicalCost,BigDecimal marginValue,BigDecimal marginPercent,boolean published,boolean featured,Instant createdAt){
  static Response from(Product p){BigDecimal margin=p.getPrice().subtract(p.getTechnicalCost()),percent=p.getTechnicalCost().signum()==0?BigDecimal.ZERO:margin.multiply(BigDecimal.valueOf(100)).divide(p.getTechnicalCost(),2,java.math.RoundingMode.HALF_UP);return new Response(p.getId(),p.getName(),p.getDescription(),p.getPrice(),p.getCategory(),p.getImageDataUrl(),p.getImage2DataUrl(),p.getImage3DataUrl(),p.getColors(),p.getSizes(),p.getTechnicalCost(),margin,percent,p.isPublished(),p.isFeatured(),p.getCreatedAt());}
 }
 public record PublicProduct(Long id,String name,String description,BigDecimal price,String category,
  String imageDataUrl,String image2DataUrl,String image3DataUrl,String colors,String sizes,boolean featured){
  static PublicProduct from(Product p){return new PublicProduct(p.getId(),p.getName(),p.getDescription(),p.getPrice(),
   p.getCategory(),p.getImageDataUrl(),p.getImage2DataUrl(),p.getImage3DataUrl(),p.getColors(),p.getSizes(),p.isFeatured());}
 }
 public record PublicCatalog(Long ownerId,String companyName,String phone,String logoDataUrl,List<PublicProduct> products){}

 @GetMapping("/api/products") @Transactional public List<Response> list(Authentication a){return products.findByOwnerEmailOrderByCreatedAtDesc(a.getName()).stream().map(Response::from).toList();}
 @GetMapping("/api/products/catalog-info") @Transactional public PublicCatalog catalogInfo(Authentication a){return catalog(users.findByEmailIgnoreCase(a.getName()).orElseThrow().getId());}
 @PostMapping("/api/products") @Transactional public ResponseEntity<Response> create(@Valid @RequestBody Request r,Authentication a){Product p=new Product();p.setOwner(users.findByEmailIgnoreCase(a.getName()).orElseThrow());apply(p,r);return ResponseEntity.status(201).body(Response.from(products.save(p)));}
 @PutMapping("/api/products/{id}") @Transactional public Response update(@PathVariable Long id,@Valid @RequestBody Request r,Authentication a){Product p=owned(id,a.getName());apply(p,r);return Response.from(products.save(p));}
 @DeleteMapping("/api/products/{id}") @Transactional public void delete(@PathVariable Long id,Authentication a){products.delete(owned(id,a.getName()));}
 public record CouponRequest(@NotBlank @Size(max=30) String code,@NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal discountPercent,boolean active){}
 public record CouponResponse(Long id,String code,BigDecimal discountPercent,boolean active){static CouponResponse from(Coupon c){return new CouponResponse(c.getId(),c.getCode(),c.getDiscountPercent(),c.isActive());}}
 @GetMapping("/api/coupons") public List<CouponResponse> coupons(Authentication a){return coupons.findByOwnerEmailOrderByCode(a.getName()).stream().map(CouponResponse::from).toList();}
 @PostMapping("/api/coupons") @Transactional public CouponResponse createCoupon(@Valid @RequestBody CouponRequest r,Authentication a){Coupon c=new Coupon();c.setOwner(users.findByEmailIgnoreCase(a.getName()).orElseThrow());applyCoupon(c,r);return CouponResponse.from(coupons.save(c));}
 @PutMapping("/api/coupons/{id}") @Transactional public CouponResponse updateCoupon(@PathVariable Long id,@Valid @RequestBody CouponRequest r,Authentication a){Coupon c=coupons.findByIdAndOwnerEmail(id,a.getName()).orElseThrow();applyCoupon(c,r);return CouponResponse.from(coupons.save(c));}
 @DeleteMapping("/api/coupons/{id}") @Transactional public void deleteCoupon(@PathVariable Long id,Authentication a){coupons.delete(coupons.findByIdAndOwnerEmail(id,a.getName()).orElseThrow());}
 @GetMapping("/api/public/catalog/{ownerId}/coupon/{code}") public CouponResponse publicCoupon(@PathVariable Long ownerId,@PathVariable String code){return CouponResponse.from(coupons.findByOwnerIdAndCodeIgnoreCaseAndActiveTrue(ownerId,code).orElseThrow(()->new NoSuchElementException("Cupom inválido ou inativo")));}
 @GetMapping("/api/public/catalog/{ownerId}") @Transactional public PublicCatalog catalog(@PathVariable Long ownerId){
  User owner=users.findById(ownerId).filter(User::isEnabled).orElseThrow();
  CompanyProfile company=companies.findByOwnerEmail(owner.getEmail()).orElse(null);
  String logo=company==null||company.getLogo()==null?null:"data:"+company.getLogoContentType()+";base64,"+Base64.getEncoder().encodeToString(company.getLogo());
  return new PublicCatalog(ownerId,company!=null&&company.getCompanyName()!=null?company.getCompanyName():owner.getName(),company==null?null:company.getPhone(),logo,
   products.findByOwnerIdAndPublishedTrueOrderByFeaturedDescCreatedAtDesc(ownerId).stream().map(PublicProduct::from).toList());
 }
 private Product owned(Long id,String email){return products.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Produto não encontrado"));}
 private void apply(Product p,Request r){p.setName(r.name().trim());p.setDescription(clean(r.description()));p.setPrice(r.price());p.setCategory(r.category().trim());p.setImageDataUrl(validImage(r.imageDataUrl()));p.setImage2DataUrl(validImage(r.image2DataUrl()));p.setImage3DataUrl(validImage(r.image3DataUrl()));p.setColors(clean(r.colors()));p.setSizes(clean(r.sizes()));p.setTechnicalCost(r.technicalCost());p.setPublished(r.published());p.setFeatured(r.featured());}
 private String validImage(String value){
  if(value==null||value.isBlank())return null;
  String image=value.trim();int comma=image.indexOf(',');
  if(comma<0)throw new IllegalArgumentException("Imagem inválida");
  String header=image.substring(0,comma).toLowerCase(Locale.ROOT);
  if(!Set.of("data:image/png;base64","data:image/jpeg;base64","data:image/webp;base64").contains(header))
   throw new IllegalArgumentException("Use imagens PNG, JPEG ou WebP");
  byte[] bytes;
  try{bytes=Base64.getDecoder().decode(image.substring(comma+1));}
  catch(IllegalArgumentException e){throw new IllegalArgumentException("Imagem inválida");}
  if(bytes.length>MAX_PRODUCT_IMAGE_BYTES)throw new IllegalArgumentException("Cada imagem deve ter no máximo 10 MB");
  boolean signature=header.contains("png")?isPng(bytes):header.contains("jpeg")?isJpeg(bytes):isWebp(bytes);
  if(!signature)throw new IllegalArgumentException("O conteúdo da imagem não corresponde ao formato informado");
  return image;
 }
 private boolean isPng(byte[] b){return b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10;}
 private boolean isJpeg(byte[] b){return b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;}
 private boolean isWebp(byte[] b){return b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P';}
 private void applyCoupon(Coupon c,CouponRequest r){c.setCode(r.code().trim().toUpperCase());c.setDiscountPercent(r.discountPercent());c.setActive(r.active());}
 private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Map<String,String>> missing(RuntimeException e){return ResponseEntity.status(404).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,String>> bad(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
}
