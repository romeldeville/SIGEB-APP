package cm.cenc.sigeb.carte.service;

import cm.cenc.sigeb.carte.entity.CarteEmise;
import cm.cenc.sigeb.carte.enums.FormatDocument;
import cm.cenc.sigeb.carte.enums.TypeEmission;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Génère à la volée le PDF officiel (Carte ou Extrait A4, FR ou EN) à partir
 * des données persistées en base (`donnees_imprimees_json` + `template_version`)
 * et du QR Code signé — zéro gestionnaire de fichiers.
 */
@Service
public class PdfCarteGeneratorService {

    private static final Color NAVY = new Color(27, 42, 74);
    private static final Color GOLD = new Color(197, 155, 39);

    public byte[] genererPdf(CarteEmise carte, String qrPayload) {
        boolean en = "EN".equalsIgnoreCase(carte.getLangue());
        Map<String, Object> data = carte.getDonneesImprimees();

        Rectangle pageSize = carte.getFormatDocument() == FormatDocument.CARTE
                ? PageSize.A6.rotate()
                : PageSize.A4;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(pageSize, 24, 24, 20, 20);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, NAVY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GOLD);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, NAVY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY);

            String entete = en
                    ? "CATHOLIC CHURCH IN CAMEROON — NATIONAL BAPTISM REGISTER (SIGEB)"
                    : "ÉGLISE CATHOLIQUE DU CAMEROUN — REGISTRE NATIONAL DE BAPTÊME (SIGEB)";
            Paragraph pHeader = new Paragraph(entete, subtitleFont);
            pHeader.setAlignment(Element.ALIGN_CENTER);
            doc.add(pHeader);

            String titre = carte.getFormatDocument() == FormatDocument.CARTE
                    ? (en ? "BAPTISM CARD" : "CARTE DE BAPTÊME")
                    : (en ? "OFFICIAL BAPTISM EXTRACT" : "EXTRAIT D'ACTE DE BAPTÊME");
            if (carte.getTypeEmission() == TypeEmission.DUPLICATA) {
                titre += " — DUPLICATA N°" + carte.getNumeroDuplicata();
            }
            Paragraph pTitle = new Paragraph(titre, titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingAfter(8);
            doc.add(pTitle);

            PdfPTable layout = new PdfPTable(new float[]{3.2f, 1.3f});
            layout.setWidthPercentage(100);

            PdfPTable infos = new PdfPTable(2);
            infos.setWidthPercentage(100);
            ajouterLigne(infos, "IUN", str(data.get("iun")), labelFont, valueFont);
            ajouterLigne(infos, en ? "Code" : "Code carte", carte.getCodeCarte(), labelFont, valueFont);
            ajouterLigne(infos, en ? "Diocese / Parish" : "Diocèse / Paroisse",
                    str(data.get("diocese")) + " / " + str(data.get("paroisse")), labelFont, valueFont);
            ajouterLigne(infos, en ? "Full Name" : "Nom & Prénoms",
                    str(data.get("nom")) + " " + str(data.get("prenoms")), labelFont, valueFont);
            ajouterLigne(infos, en ? "Born" : "Né(e) le / à",
                    str(data.get("dateNaissance")) + " — " + str(data.get("lieuNaissance")), labelFont, valueFont);
            ajouterLigne(infos, en ? "Baptized" : "Baptisé(e) le",
                    str(data.get("dateBapteme")) + " (" + str(data.get("lieuBapteme")) + ")", labelFont, valueFont);
            ajouterLigne(infos, en ? "Minister" : "Ministre", str(data.get("ministreCelebrant")), labelFont, valueFont);
            ajouterLigne(infos, en ? "Paper Register" : "Registre papier", str(data.get("referenceRegistre")), labelFont, valueFont);

            PdfPCell leftCell = new PdfPCell(infos);
            leftCell.setBorder(Rectangle.NO_BORDER);
            layout.addCell(leftCell);

            Image qrImg = Image.getInstance(genererQrPng(qrPayload, 160));
            qrImg.scaleToFit(95, 95);
            PdfPCell qrCell = new PdfPCell(qrImg, false);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            layout.addCell(qrCell);

            doc.add(layout);

            Object mentionsObj = data.get("mentions");
            if (mentionsObj instanceof List<?> mentions && !mentions.isEmpty()) {
                Paragraph mTitle = new Paragraph(en ? "Marginal Notations:" : "Mentions marginales :", labelFont);
                mTitle.setSpacingBefore(6);
                doc.add(mTitle);
                for (Object m : mentions) {
                    doc.add(new Paragraph("• " + m.toString(), valueFont));
                }
            }

            Paragraph pFooter = new Paragraph(
                    "Template " + carte.getTemplateVersion() + " · " + carte.getEmisLe() + " · " + qrPayload.substring(0, Math.min(28, qrPayload.length())) + "...",
                    footerFont
            );
            pFooter.setSpacingBefore(8);
            pFooter.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFooter);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur génération PDF à la volée", e);
        }
    }

    private void ajouterLigne(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(2);
        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(2);
        table.addCell(c1);
        table.addCell(c2);
    }

    private byte[] genererQrPng(String payload, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        }
    }

    private static String str(Object o) {
        return o == null ? "-" : o.toString();
    }
}
