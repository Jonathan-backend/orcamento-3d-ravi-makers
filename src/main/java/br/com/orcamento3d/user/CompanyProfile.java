package br.com.orcamento3d.user;
import jakarta.persistence.*;
@Entity @Table(name="company_profiles")
public class CompanyProfile{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(unique=true) private User owner;
 @Column(length=140) private String responsibleName;@Column(length=140) private String companyName;
 @Column(length=20) private String taxId;@Column(length=30) private String phone;
 @Column(length=140) private String commercialEmail;@Column(length=250) private String website;
 @Column(length=300) private String address;
 @Lob private byte[] logo;@Column(length=50) private String logoContentType;
 public User getOwner(){return owner;}public void setOwner(User v){owner=v;}public String getResponsibleName(){return responsibleName;}public void setResponsibleName(String v){responsibleName=v;}public String getCompanyName(){return companyName;}public void setCompanyName(String v){companyName=v;}public String getTaxId(){return taxId;}public void setTaxId(String v){taxId=v;}public String getPhone(){return phone;}public void setPhone(String v){phone=v;}public String getCommercialEmail(){return commercialEmail;}public void setCommercialEmail(String v){commercialEmail=v;}public String getWebsite(){return website;}public void setWebsite(String v){website=v;}public String getAddress(){return address;}public void setAddress(String v){address=v;}public byte[] getLogo(){return logo;}public void setLogo(byte[] v){logo=v;}public String getLogoContentType(){return logoContentType;}public void setLogoContentType(String v){logoContentType=v;}
}
