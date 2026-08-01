package br.com.orcamento3d.customer;
import br.com.orcamento3d.customer.CustomerDtos.*;
import br.com.orcamento3d.user.UserRepository;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class CustomerService{
 private final CustomerRepository customers;private final UserRepository users;
 public CustomerService(CustomerRepository customers,UserRepository users){this.customers=customers;this.users=users;}
 public ListResponse list(String email){var rows=customers.findByOwnerEmailOrderByNameAsc(email);return new ListResponse(rows.size(),(int)rows.stream().filter(Customer::isActive).count(),rows.stream().map(Response::from).toList());}
 public Response create(Request r,String email){Customer c=new Customer();c.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());apply(c,r);return Response.from(customers.save(c));}
 public Response update(Long id,Request r,String email){Customer c=owned(id,email);apply(c,r);return Response.from(customers.save(c));}
 public void delete(Long id,String email){customers.delete(owned(id,email));}
 private Customer owned(Long id,String email){return customers.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Cliente não encontrado"));}
 private void apply(Customer c,Request r){c.setPersonType(r.personType());c.setName(r.name().trim());c.setTradeName(clean(r.tradeName()));c.setDocument(digits(r.document()));c.setStateRegistration(clean(r.stateRegistration()));c.setEmail(clean(r.email()));c.setPhone(clean(r.phone()));c.setWhatsapp(clean(r.whatsapp()));c.setPostalCode(clean(r.postalCode()));c.setStreet(clean(r.street()));c.setAddressNumber(clean(r.addressNumber()));c.setComplement(clean(r.complement()));c.setDistrict(clean(r.district()));c.setCity(clean(r.city()));c.setAddressState(r.addressState()==null?null:r.addressState().trim().toUpperCase());c.setNotes(clean(r.notes()));c.setActive(r.active());}
 private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
 private String digits(String v){String clean=clean(v);return clean==null?null:clean.replaceAll("\\D","");}
}
