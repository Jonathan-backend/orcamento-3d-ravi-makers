package br.com.orcamento3d.budget;
import br.com.orcamento3d.customer.Customer;
import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="budgets")
public class Budget{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private User owner;
 @ManyToOne(fetch=FetchType.LAZY) private Customer customer;
 @Column(nullable=false,length=140) private String title;
 @Column(nullable=false,precision=7,scale=2) private BigDecimal marginPercent;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal costTotal=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal maintenanceCost=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal machineCost=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal laborCost=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal additionalCost=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal fixedCost=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal failureCost=BigDecimal.ZERO;
 @Column(nullable=false,length=30,columnDefinition="varchar(30) default 'STANDARD_SALE'") private String purpose="STANDARD_SALE";
 @Column(nullable=false,length=20) private String status="DRAFT";
 @OneToMany(mappedBy="budget",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("position")
 private List<BudgetPlate> plates=new ArrayList<>();
 @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
 public Customer getCustomer(){return customer;} public void setCustomer(Customer v){customer=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public BigDecimal getMarginPercent(){return marginPercent;} public void setMarginPercent(BigDecimal v){marginPercent=v;}
 public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
 public BigDecimal getCostTotal(){return costTotal;} public void setCostTotal(BigDecimal v){costTotal=v;}
 public BigDecimal getMaintenanceCost(){return maintenanceCost;} public void setMaintenanceCost(BigDecimal v){maintenanceCost=v;}
 public BigDecimal getMachineCost(){return machineCost;} public void setMachineCost(BigDecimal v){machineCost=v;}
 public BigDecimal getLaborCost(){return laborCost;} public void setLaborCost(BigDecimal v){laborCost=v;}
 public BigDecimal getAdditionalCost(){return additionalCost;} public void setAdditionalCost(BigDecimal v){additionalCost=v;}
 public BigDecimal getFixedCost(){return fixedCost;} public void setFixedCost(BigDecimal v){fixedCost=v;}
 public BigDecimal getFailureCost(){return failureCost;} public void setFailureCost(BigDecimal v){failureCost=v;}
 public String getPurpose(){return purpose;} public void setPurpose(String v){purpose=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public List<BudgetPlate> getPlates(){return plates;} public Instant getCreatedAt(){return createdAt;}
}
