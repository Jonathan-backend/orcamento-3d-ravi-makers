package br.com.orcamento3d.budget;

import br.com.orcamento3d.customer.Customer;
import br.com.orcamento3d.user.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BudgetPdfService {
    private static final PDType1Font REGULAR=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float ORANGE_R=1f,ORANGE_G=.36f,ORANGE_B=0f;
    private final CompanyProfileRepository companies;
    private final BudgetRepository budgets;

    public BudgetPdfService(CompanyProfileRepository companies,BudgetRepository budgets){this.companies=companies;this.budgets=budgets;}

    @Transactional public byte[] generate(Long budgetId,String email)throws IOException{
        Budget budget=budgets.findByIdAndOwnerEmail(budgetId,email).orElseThrow(()->new java.util.NoSuchElementException("Orçamento não encontrado"));
        CompanyProfile company=companies.findByOwnerEmail(email).orElse(null);
        try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            PageWriter page=new PageWriter(document,company);
            page.header("ORÇAMENTO #"+budget.getId());
            page.labelValue("Data",DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo")).format(budget.getCreatedAt()));
            page.labelValue("Proposta",budget.getTitle());
            Customer customer=budget.getCustomer();
            page.labelValue("Cliente",customer==null?"Consumidor não identificado":customer.getName());
            if(customer!=null&&customer.getDocument()!=null)page.labelValue("CPF/CNPJ",customer.getDocument());
            if(customer!=null&&customer.getEmail()!=null)page.labelValue("E-mail",customer.getEmail());
            if(customer!=null&&(customer.getWhatsapp()!=null||customer.getPhone()!=null))
                page.labelValue("Contato",customer.getWhatsapp()!=null?customer.getWhatsapp():customer.getPhone());
            page.section("ITENS DA PROPOSTA");
            int number=0;
            for(BudgetPlate plate:budget.getPlates()){
                page.ensure(52);
                page.text("Item "+(++number)+" - Peça do projeto",BOLD,11,35);
                page.text("Quantidade: 1   Serviço: fabricação por impressão 3D",REGULAR,9,20);
            }
            page.section("RESUMO FINANCEIRO");
            page.amount("VALOR TOTAL",budget.getTotal(),true);
            page.ensure(80);
            page.text("Condições",BOLD,10,25);
            page.text("Proposta sujeita à confirmação do modelo, acabamento, cores e prazo de entrega.",REGULAR,9,18);
            page.text("Produção iniciada após a aprovação do orçamento.",REGULAR,9,18);
            page.text("Obrigado por escolher a RAVI MAKERS.",BOLD,10,18);
            page.finish();
            document.save(output);return output.toByteArray();
        }
    }

    private String money(BigDecimal v){return java.text.NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(v);}
    private String decimal(double v){return String.format(new Locale("pt","BR"),"%.2f",v);}
    private String duration(double minutes){int total=(int)Math.round(minutes);return total/60+"h "+total%60+"min";}

    private static final class PageWriter{
        private final PDDocument doc;private final CompanyProfile company;private PDPage page;private PDPageContentStream out;private float y;
        PageWriter(PDDocument doc,CompanyProfile company)throws IOException{this.doc=doc;this.company=company;newPage();}
        void newPage()throws IOException{
            if(out!=null)out.close();page=new PDPage(PDRectangle.A4);doc.addPage(page);out=new PDPageContentStream(doc,page);y=806;
            fill(8,8,9);out.addRect(0,742,595,100);out.fill();
            try{
                byte[] logo=company!=null&&company.getLogo()!=null?company.getLogo():new ClassPathResource("static/img/ravi-makers.png").getInputStream().readAllBytes();
                PDImageXObject image=PDImageXObject.createFromByteArray(doc,logo,"logo");out.drawImage(image,34,758,155,68);
            }catch(Exception ignored){}
            fill(255,94,0);textAt("PROPOSTA COMERCIAL",BOLD,10,410,794);
            fill(190,190,195);textAt(company!=null&&company.getCompanyName()!=null?company.getCompanyName():"RAVI MAKERS",REGULAR,8,410,778);
            y=718;
        }
        void header(String value)throws IOException{text(value,BOLD,21,32);line();y-=8;}
        void section(String value)throws IOException{ensure(44);y-=7;fill(255,94,0);text(value,BOLD,10,27);line();}
        void labelValue(String label,String value)throws IOException{if(value==null||value.isBlank())return;ensure(25);fill(105,105,112);textAt(label.toUpperCase(Locale.ROOT),BOLD,7,35,y);fill(25,25,28);textAt(clean(value),REGULAR,10,135,y);y-=20;}
        void amount(String label,BigDecimal value,boolean total)throws IOException{amount(label,value,total,null);}
        void amount(String label,BigDecimal value,boolean total,String suffix)throws IOException{
            ensure(28);if(total){fill(255,94,0);out.addRect(320,y-7,240,28);out.fill();fill(255,255,255);}
            else fill(35,35,38);
            textAt(label,total?BOLD:REGULAR,total?11:9,total?335:350,y+2);
            String formatted=suffix==null?java.text.NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(value):value.toPlainString()+suffix;
            textAt(formatted,BOLD,total?13:10,480,y+2);y-=total?38:24;
        }
        void text(String value,PDFont font,float size,float gap)throws IOException{ensure(gap+6);fill(35,35,38);textAt(clean(value),font,size,35,y);y-=gap;}
        void line()throws IOException{out.setStrokingColor(225/255f,225/255f,228/255f);out.moveTo(35,y);out.lineTo(560,y);out.stroke();y-=16;}
        void ensure(float height)throws IOException{if(y-height<55)newPage();}
        void finish()throws IOException{
            fill(80,80,85);textAt("RAVI MAKERS - Gestão profissional para impressão 3D",REGULAR,7,35,28);
            if(company!=null&&company.getPhone()!=null)textAt(company.getPhone(),REGULAR,7,460,28);out.close();out=null;
        }
        void textAt(String value,PDFont font,float size,float x,float y)throws IOException{
            out.beginText();out.setFont(font,size);out.newLineAtOffset(x,y);out.showText(clean(value));out.endText();
        }
        void fill(int r,int g,int b)throws IOException{out.setNonStrokingColor(r/255f,g/255f,b/255f);}
        static String clean(String value){return value==null?"":value.replace('\n',' ').replace('\r',' ').replace("–","-").replace("—","-");}
    }
}
