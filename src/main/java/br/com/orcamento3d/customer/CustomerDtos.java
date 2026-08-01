package br.com.orcamento3d.customer;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public final class CustomerDtos{
 private CustomerDtos(){}
 public record Request(@NotBlank @Pattern(regexp="PF|PJ") String personType,
  @NotBlank @Size(max=140) String name,@Size(max=140) String tradeName,
  @Size(max=20) String document,@Size(max=20) String stateRegistration,
  @Email @Size(max=140) String email,@Size(max=30) String phone,@Size(max=30) String whatsapp,
  @Size(max=12) String postalCode,@Size(max=140) String street,@Size(max=20) String addressNumber,
  @Size(max=100) String complement,@Size(max=100) String district,@Size(max=100) String city,
  @Size(max=2) String addressState,@Size(max=500) String notes,boolean active){}
 public record Response(Long id,String personType,String name,String tradeName,String document,
  String stateRegistration,String email,String phone,String whatsapp,String postalCode,String street,
  String addressNumber,String complement,String district,String city,String addressState,String notes,
  boolean active,Instant createdAt,Instant updatedAt){
  static Response from(Customer c){return new Response(c.getId(),c.getPersonType(),c.getName(),c.getTradeName(),
   c.getDocument(),c.getStateRegistration(),c.getEmail(),c.getPhone(),c.getWhatsapp(),c.getPostalCode(),
   c.getStreet(),c.getAddressNumber(),c.getComplement(),c.getDistrict(),c.getCity(),c.getAddressState(),
   c.getNotes(),c.isActive(),c.getCreatedAt(),c.getUpdatedAt());}
 }
 public record ListResponse(int total,int active,List<Response> customers){}
}
