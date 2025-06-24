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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pdfCorte")
public class PdfCorte {

    @Autowired
    private CorteRepository corteRepository;

    @PostMapping("/generar")
    public void generarPdf(
            @RequestParam("selectedRows") List<String> ids,
            @RequestParam("total") int total,
            HttpServletResponse response) throws IOException, DocumentException {

        if (ids == null || ids.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se seleccionaron registros para el PDF.");
            return;
        }

        List<Corte> cortes = corteRepository.findAllById(ids);

        if (cortes.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No se encontraron registros con los IDs proporcionados.");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_actividades_corte.pdf\"");

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        try {
            agregarTitulo(document, "Reporte de Actividades - Corte");
            agregarInformacionGeneral(document, cortes);
            agregarTablaDatos(document, cortes);
            agregarGraficaMedidas(document, cortes);
            agregarGraficaProveedor(document, cortes);
            agregarTotalCalculado(document, total);
        } catch (Exception e) {
            System.err.println("Error generando PDF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

    private void agregarTitulo(Document document, String titulo) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph paragraph = new Paragraph(titulo, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(20);
        document.add(paragraph);

        // Agregar fecha de generación
        Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph fechaParagraph = new Paragraph("Generado el: " + new Date().toString(), fechaFont);
        fechaParagraph.setAlignment(Element.ALIGN_CENTER);
        fechaParagraph.setSpacingAfter(20);
        document.add(fechaParagraph);
    }

    private void agregarInformacionGeneral(Document document, List<Corte> cortes) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);

        // Calcular totales
        int totalJuegos = cortes.stream()
                .mapToInt(c -> {
                    try {
                        return Integer.parseInt(c.getJuegos());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .sum();

        Set<String> medidasUnicas = cortes.stream()
                .map(Corte::getMedidas)
                .collect(Collectors.toSet());

        String empleado = cortes.isEmpty() ? "N/A" : cortes.get(0).getUsuario().getNombre();

        Paragraph info = new Paragraph();
        info.add(new Chunk("Resumen del Reporte\n", font));
        info.add(new Chunk("Empleado: " + empleado + "\n", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        info.add(new Chunk("Total de registros: " + cortes.size() + "\n",
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        info.add(new Chunk("Total de juegos cortados: " + totalJuegos + "\n",
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        info.add(new Chunk("Medidas trabajadas: " + medidasUnicas.size() + " tipos diferentes\n",
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        info.setSpacingAfter(20);

        document.add(info);
    }

    private void agregarTablaDatos(Document document, List<Corte> cortes) throws DocumentException {
        PdfPTable tabla = new PdfPTable(7); // 7 columnas
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(15);
        tabla.setSpacingAfter(20);

        // Establecer anchos de columnas
        float[] columnWidths = { 12f, 18f, 10f, 15f, 20f, 15f, 10f };
        tabla.setWidths(columnWidths);

        String[] encabezados = { "Fecha", "Medidas", "Juegos", "Proveedor", "Novedades", "Evidencia", "Empleado" };
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);

        for (String encabezado : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado, headerFont));
            celda.setBackgroundColor(new BaseColor(52, 73, 94)); // Color azul oscuro
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celda.setPadding(8);
            tabla.addCell(celda);
        }

        // ✅ CORRECCIÓN: Usar DateTimeFormatter para LocalDate
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        for (Corte corte : cortes) {
            // Fecha - CORREGIDO para LocalDate
            String fechaStr = (corte.getFecha() != null) ? corte.getFecha().format(formatoFecha) : "Sin fecha";
            tabla.addCell(new Phrase(fechaStr, cellFont));

            // Medidas
            tabla.addCell(new Phrase(corte.getMedidas() != null ? corte.getMedidas() : "N/A", cellFont));

            // Juegos
            tabla.addCell(new Phrase(corte.getJuegos() != null ? corte.getJuegos() : "0", cellFont));

            // Proveedor
            tabla.addCell(new Phrase(corte.getProveedor() != null ? corte.getProveedor() : "N/A", cellFont));

            // Novedades
            String novedades = corte.getNovedades() != null ? corte.getNovedades() : "Sin novedades";
            if (novedades.length() > 50) {
                novedades = novedades.substring(0, 47) + "...";
            }
            tabla.addCell(new Phrase(novedades, cellFont));

            // Imagen/Evidencia
            if (corte.getImagen() != null && corte.getImagen().length > 0) {
                try {
                    Image imagen = Image.getInstance(corte.getImagen());
                    imagen.scaleToFit(40, 40);
                    PdfPCell celdaImagen = new PdfPCell(imagen, true);
                    celdaImagen.setFixedHeight(45);
                    celdaImagen.setHorizontalAlignment(Element.ALIGN_CENTER);
                    celdaImagen.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    tabla.addCell(celdaImagen);
                } catch (Exception e) {
                    System.err.println("Error procesando imagen: " + e.getMessage());
                    tabla.addCell(new Phrase("Error imagen", cellFont));
                }
            } else {
                tabla.addCell(new Phrase("Sin imagen", cellFont));
            }

            // Empleado
            String nombreEmpleado = (corte.getUsuario() != null && corte.getUsuario().getNombre() != null)
                    ? corte.getUsuario().getNombre()
                    : "N/A";
            tabla.addCell(new Phrase(nombreEmpleado, cellFont));
        }

        document.add(tabla);
    }

    private void agregarGraficaMedidas(Document document, List<Corte> cortes)
            throws DocumentException, IOException {

        Map<String, Integer> trabajoPorMedida = cortes.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getMedidas() != null ? c.getMedidas() : "Sin medida",
                        Collectors.summingInt(c -> {
                            try {
                                return Integer.parseInt(c.getJuegos());
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        })));

        if (trabajoPorMedida.isEmpty()) {
            return; // No hay datos para graficar
        }

        DefaultPieDataset dataset = new DefaultPieDataset();
        trabajoPorMedida.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                "Distribución por Medidas de Sábanas",
                dataset,
                true,
                true,
                false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} juegos ({2})"));
        plot.setSimpleLabels(true);

        // Colores personalizados
        plot.setSectionPaint("Sencilla", new java.awt.Color(52, 152, 219));
        plot.setSectionPaint("Semi", new java.awt.Color(46, 204, 113));
        plot.setSectionPaint("Doble", new java.awt.Color(155, 89, 182));
        plot.setSectionPaint("Queen", new java.awt.Color(241, 196, 15));
        plot.setSectionPaint("King", new java.awt.Color(231, 76, 60));

        agregarGraficaAlDocumento(document, chart, 500, 300);
    }

    private void agregarGraficaProveedor(Document document, List<Corte> cortes)
            throws DocumentException, IOException {

        Map<String, Integer> trabajoPorProveedor = cortes.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getProveedor() != null ? c.getProveedor() : "Sin proveedor",
                        Collectors.summingInt(c -> {
                            try {
                                return Integer.parseInt(c.getJuegos());
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        })));

        if (trabajoPorProveedor.isEmpty() || trabajoPorProveedor.size() <= 1) {
            return; // No hay suficientes datos para una gráfica útil
        }

        DefaultPieDataset dataset = new DefaultPieDataset();
        trabajoPorProveedor.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                "Distribución por Proveedor",
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

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            org.jfree.chart.ChartUtils.writeChartAsPNG(outputStream, chart, ancho, alto);

            Image imagenGrafica = Image.getInstance(outputStream.toByteArray());
            imagenGrafica.setAlignment(Image.MIDDLE);
            imagenGrafica.setSpacingBefore(15);
            imagenGrafica.setSpacingAfter(15);

            // Escalar la imagen si es necesario
            if (imagenGrafica.getWidth() > 500) {
                imagenGrafica.scaleToFit(500, 300);
            }

            document.add(imagenGrafica);
        } catch (Exception e) {
            System.err.println("Error agregando gráfica: " + e.getMessage());
            // Agregar un mensaje en lugar de la gráfica si hay error
            Font font = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.RED);
            Paragraph errorMsg = new Paragraph("Error generando gráfica: " + e.getMessage(), font);
            errorMsg.setAlignment(Element.ALIGN_CENTER);
            document.add(errorMsg);
        }
    }

    private void agregarTotalCalculado(Document document, int total) throws DocumentException {
        // Crear una línea separadora
        Paragraph linea = new Paragraph("_".repeat(80));
        linea.setAlignment(Element.ALIGN_CENTER);
        linea.setSpacingBefore(20);
        linea.setSpacingAfter(10);
        document.add(linea);

        // Total calculado
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLUE);
        Paragraph parrafoTotal = new Paragraph("TOTAL CALCULADO: $" + String.format("%,d", total), fontTotal);
        parrafoTotal.setAlignment(Element.ALIGN_RIGHT);
        parrafoTotal.setSpacingBefore(10);
        parrafoTotal.setSpacingAfter(20);
        document.add(parrafoTotal);

        // Información adicional
        Font fontInfo = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
        Paragraph infoFinal = new Paragraph(
                "* Este cálculo se basa en los registros seleccionados y los precios configurados en el sistema.",
                fontInfo);
        infoFinal.setAlignment(Element.ALIGN_RIGHT);
        document.add(infoFinal);
    }
}