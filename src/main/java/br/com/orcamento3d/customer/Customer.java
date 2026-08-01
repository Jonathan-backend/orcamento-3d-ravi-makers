package br.com.orcamento3d.customer;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="customers")
public class Customer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY)
    private User owner;
    @Column(nullable=false,length=20) private String personType;
    @Column(nullable=false,length=140) private String name;
    @Column(length=140) private String tradeName;
    @Column(length=20) private String document;
    @Column(length=20) private String stateRegistration;
    @Column(length=140) private String email;
    @Column(length=30) private String phone;
    @Column(length=30) private String whatsapp;
    @Column(length=12) private String postalCode;
    @Column(length=140) private String street;
    @Column(length=20) private String addressNumber;
    @Column(length=100) private String complement;
    @Column(length=100) private String district;
    @Column(length=100) private String city;
    @Column(length=2) private String addressState;
    @Column(length=500) private String notes;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(nullable=false) private Instant updatedAt=Instant.now();
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public Long getId(){return id;} public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
    public String getPersonType(){return personType;} public void setPersonType(String v){personType=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getTradeName(){return tradeName;} public void setTradeName(String v){tradeName=v;}
    public String getDocument(){return document;} public void setDocument(String v){document=v;}
    public String getStateRegistration(){return stateRegistration;} public void setStateRegistration(String v){stateRegistration=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getWhatsapp(){return whatsapp;} public void setWhatsapp(String v){whatsapp=v;}
    public String getPostalCode(){return postalCode;} public void setPostalCode(String v){postalCode=v;}
    public String getStreet(){return street;} public void setStreet(String v){street=v;}
    public String getAddressNumber(){return addressNumber;} public void setAddressNumber(String v){addressNumber=v;}
    public String getComplement(){return complement;} public void setComplement(String v){complement=v;}
    public String getDistrict(){return district;} public void setDistrict(String v){district=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;}
    public String getAddressState(){return addressState;} public void setAddressState(String v){addressState=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
