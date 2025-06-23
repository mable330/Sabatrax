package com.example.demo.controller;

import com.example.demo.model.Corte;
import com.example.demo.repository.CorteRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pdfCorte")
public class PdfCorte {

    @Autowired
    private CorteRepository corteRepository;

    @PostMapping("/generar")
    public void generarPdf(@RequestParam("selectedRows") List<String> ids,
            @RequestParam("total") int total,
            HttpServletResponse response) throws IOException, DocumentException {

        List<Corte> cortes = corteRepository.findAllById(ids);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_actividades_corte.pdf\"");

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        agregarTitulo(document, "Reporte de Actividades - Corte");

        agregarTablaDatos(document, cortes);

        agregarGraficaDiasTrabajo(document, cortes);

        agregarGraficaMedidas(document, cortes);

        agregarTotalCalculado(document, total);

        document.close();
    }

    private void agregarTitulo(Document document, String titulo) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph paragraph = new Paragraph(titulo, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(20);
        document.add(paragraph);
    }

    private void agregarTablaDatos(Document document, List<Corte> cortes) throws DocumentException {
        PdfPTable tabla = new PdfPTable(7); // 7 columnas
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(15);
        tabla.setSpacingAfter(20);

        String[] encabezados = { "Fecha", "Medidas", "Juegos", "Proveedor", "Novedades", "Evidencia", "Empleado" };
        for (String encabezado : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado));
            celda.setBackgroundColor(new BaseColor(200, 200, 200));
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

        for (Corte corte : cortes) {
            tabla.addCell(new Phrase(formatoFecha.format(corte.getFecha())));
            tabla.addCell(new Phrase(corte.getMedidas()));
            tabla.addCell(new Phrase(String.valueOf(corte.getJuegos())));
            tabla.addCell(new Phrase(corte.getProveedor()));
            tabla.addCell(new Phrase(corte.getNovedades()));

            // Imagen
            if (corte.getImagen() != null) {
                try {
                    Image imagen = Image.getInstance(corte.getImagen());
                    imagen.scaleToFit(50, 50);
                    PdfPCell celdaImagen = new PdfPCell(imagen, true);
                    celdaImagen.setFixedHeight(55);
                    celdaImagen.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabla.addCell(celdaImagen);
                } catch (Exception e) {
                    tabla.addCell(new Phrase("Imagen no disponible"));
                }
            } else {
                tabla.addCell(new Phrase("Sin imagen"));
            }

            tabla.addCell(new Phrase(corte.getUsuario().getNombre()));
        }

        document.add(tabla);
    }

    private void agregarGraficaDiasTrabajo(Document document, List<Corte> cortes)
            throws DocumentException, IOException {

        Map<String, Integer> trabajoPorMedida = cortes.stream()
                .collect(Collectors.groupingBy(
                        Corte::getMedidas,
                        Collectors.summingInt(c -> {
                            try {
                                return Integer.parseInt(c.getJuegos()); // 👈 Convertimos String a int
                            } catch (NumberFormatException e) {
                                return 0; // 👈 Si hay error de formato, sumamos como 0
                            }
                        })));

        DefaultPieDataset dataset = new DefaultPieDataset();
        trabajoPorMedida.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                "Días con mayor producción (Corte)",
                dataset,
                true,
                true,
                false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} juegos ({2})"));
        plot.setSimpleLabels(true);

        agregarGraficaAlDocumento(document, chart, 500, 300);
    }

    private void agregarGraficaMedidas(Document document, List<Corte> cortes)
            throws DocumentException, IOException {

        Map<String, Integer> trabajoPorMedida = cortes.stream()
                .collect(Collectors.groupingBy(
                        Corte::getMedidas,
                        Collectors.summingInt(c -> {
                            try {
                                return Integer.parseInt(c.getJuegos()); // 👈 Convertimos String a int
                            } catch (NumberFormatException e) {
                                return 0; // 👈 Si hay error de formato, sumamos como 0
                            }
                        })));

        DefaultPieDataset dataset = new DefaultPieDataset();
        trabajoPorMedida.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                "Distribución por Medidas de Sábanas (Corte)",
                dataset,
                true,
                true,
                false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} juegos ({2})"));
        plot.setSimpleLabels(true);

        agregarGraficaAlDocumento(document, chart, 500, 300);
    }

    private void agregarGraficaAlDocumento(Document document, JFreeChart chart, int ancho, int alto)
            throws IOException, DocumentException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        org.jfree.chart.ChartUtils.writeChartAsPNG(outputStream, chart, ancho, alto);

        Image imagenGrafica = Image.getInstance(outputStream.toByteArray());
        imagenGrafica.setAlignment(Image.MIDDLE);
        imagenGrafica.setSpacingBefore(15);
        imagenGrafica.setSpacingAfter(15);
        document.add(imagenGrafica);
    }

    private void agregarTotalCalculado(Document document, int total) throws DocumentException {
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLUE);
        Paragraph parrafoTotal = new Paragraph("Total calculado: $" + String.format("%,d", total), fontTotal);
        parrafoTotal.setAlignment(Element.ALIGN_RIGHT);
        parrafoTotal.setSpacingBefore(20);
        document.add(parrafoTotal);
    }
}
