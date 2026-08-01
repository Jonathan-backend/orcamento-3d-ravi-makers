package br.com.orcamento3d.user;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;

@RestController @RequestMapping("/api/settings/company")
public class CompanyProfileController{
 private final CompanyProfileRepository profiles;private final UserRepository users;
 public CompanyProfileController(CompanyProfileRepository p,UserRepository u){profiles=p;users=u;}
 public record Data(@Size(max=140) String responsibleName,@Size(max=140) String companyName,@Size(max=20) String taxId,@Size(max=30) String phone,@Email @Size(max=140) String commercialEmail,@Size(max=250) String website,@Size(max=300) String address,String logoDataUrl){}
 @GetMapping @Transactional public Data get(Authentication a){return from(profile(a.getName()));}
 @PutMapping @Transactional public Data update(@Valid @RequestBody Data d,Authentication a){CompanyProfile p=profile(a.getName());p.setResponsibleName(clean(d.responsibleName()));p.setCompanyName(clean(d.companyName()));p.setTaxId(clean(d.taxId()));p.setPhone(clean(d.phone()));p.setCommercialEmail(clean(d.commercialEmail()));p.setWebsite(clean(d.website()));p.setAddress(clean(d.address()));if(d.logoDataUrl()!=null&&!d.logoDataUrl().isBlank()){int comma=d.logoDataUrl().indexOf(',');if(comma<0)throw new IllegalArgumentException("Logomarca inválida");String header=d.logoDataUrl().substring(0,comma);if(!(header.contains("image/png")||header.contains("image/jpeg")))throw new IllegalArgumentException("Use uma imagem PNG ou JPG");byte[] bytes=Base64.getDecoder().decode(d.logoDataUrl().substring(comma+1));if(bytes.length>1_500_000)throw new IllegalArgumentException("A logomarca deve ter no máximo 1,5 MB");p.setLogo(bytes);p.setLogoContentType(header.contains("png")?"image/png":"image/jpeg");}return from(profiles.save(p));}
 private CompanyProfile profile(String email){return profiles.findByOwnerEmail(email).orElseGet(()->{CompanyProfile p=new CompanyProfile();p.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());return profiles.save(p);});}
 private Data from(CompanyProfile p){String logo=p.getLogo()==null?null:"data:"+p.getLogoContentType()+";base64,"+Base64.getEncoder().encodeToString(p.getLogo());return new Data(p.getResponsibleName(),p.getCompanyName(),p.getTaxId(),p.getPhone(),p.getCommercialEmail(),p.getWebsite(),p.getAddress(),logo);}
 private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) public java.util.Map<String,String> bad(IllegalArgumentException e){return java.util.Map.of("message",e.getMessage());}
}
