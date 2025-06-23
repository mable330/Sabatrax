package com.example.demo.controller;

import com.example.demo.model.Maquina;
import com.example.demo.repository.MaquinaRepository;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pdf")
public class Pdf {

    @Autowired
    private MaquinaRepository maquinaRepository;

    @PostMapping("/generar")
    public void generarPdf(@RequestParam("selectedRows") List<String> ids,
            @RequestParam("total") int total,
            HttpServletResponse response) throws IOException, DocumentException {

        // Obtener las máquinas seleccionadas
        List<Maquina> maquinas = maquinaRepository.findAllById(ids);

        // Configurar respuesta HTTP
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_actividades.pdf\"");

        // Crear documento PDF
        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // 1. Título del documento
        agregarTitulo(document, "Reporte Detallado de Actividades");

        // 2. Tabla con todos los datos
        agregarTablaDatos(document, maquinas);

        // 3. Gráfico de días con más trabajo (por cantidad)
        agregarGraficaDiasTrabajo(document, maquinas);

        // 4. Gráfico de distribución por tipo de sábanas
        agregarGraficaTiposSabanas(document, maquinas);

        // 5. Resumen con el total calculado
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

    private void agregarTablaDatos(Document document, List<Maquina> maquinas) throws DocumentException {
        PdfPTable tabla = new PdfPTable(7); // 7 columnas
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(15);
        tabla.setSpacingAfter(20);

        // Encabezados de tabla
        String[] encabezados = { "Fecha", "Medidas", "Tipo Sábanas", "Proveedor", "Novedades", "Cantidad",
                "Evidencia" };
        for (String encabezado : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(encabezado));
            celda.setBackgroundColor(new BaseColor(200, 200, 200));
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }

        // Datos de las máquinas
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        for (Maquina maquina : maquinas) {
            // Fecha
            tabla.addCell(new Phrase(formatoFecha.format(maquina.getFecha())));

            // Medidas
            tabla.addCell(new Phrase(maquina.getMedidas()));

            // Tipo de sábanas
            tabla.addCell(new Phrase(maquina.getTipo_sabanas()));

            // Proveedor
            tabla.addCell(new Phrase(maquina.getProveedor()));

            // Novedades
            tabla.addCell(new Phrase(maquina.getNovedades()));

            // Cantidad
            PdfPCell celdaCantidad = new PdfPCell(new Phrase(String.valueOf(maquina.getCantidad())));
            celdaCantidad.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(celdaCantidad);

            // Evidencia (imagen)
            if (maquina.getImagen() != null) {
                try {
                    Image imagen = Image.getInstance(maquina.getImagen());
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
        }

        document.add(tabla);
    }

    private void agregarGraficaDiasTrabajo(Document document, List<Maquina> maquinas)
            throws DocumentException, IOException {
        // Agrupar por día de la semana y sumar cantidades
        Map<String, Integer> trabajoPorDia = maquinas.stream()
                .collect(Collectors.groupingBy(
                        m -> new SimpleDateFormat("EEEE", new Locale("es", "ES")).format(m.getFecha()),
                        Collectors.summingInt(Maquina::getCantidad)));

        // Crear dataset para la gráfica
        DefaultPieDataset dataset = new DefaultPieDataset();
        trabajoPorDia.forEach((dia, cantidad) -> dataset.setValue(dia, cantidad));

        // Crear gráfica circular
        JFreeChart chart = ChartFactory.createPieChart(
                "Días con mayor producción",
                dataset,
                true,
                true,
                false);

        // Personalizar gráfica
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})",
                new Locale("es", "ES")));
        plot.setSimpleLabels(true);

        // Convertir gráfica a imagen y añadir al PDF
        agregarGraficaAlDocumento(document, chart, 500, 300);
    }

    private void agregarGraficaTiposSabanas(Document document, List<Maquina> maquinas)
            throws DocumentException, IOException {
        // Agrupar por tipo de sábanas y sumar cantidades
        Map<String, Integer> trabajoPorTipo = maquinas.stream()
                .collect(Collectors.groupingBy(
                        Maquina::getTipo_sabanas,
                        Collectors.summingInt(Maquina::getCantidad)));

        // Crear dataset para la gráfica
        DefaultPieDataset dataset = new DefaultPieDataset();
        trabajoPorTipo.forEach((tipo, cantidad) -> dataset.setValue(tipo, cantidad));

        // Crear gráfica circular
        JFreeChart chart = ChartFactory.createPieChart(
                "Mayor cantidad de tipos de sabanas realizadas",
                dataset,
                true,
                true,
                false);

        // Personalizar gráfica
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})",
                new Locale("es", "ES")));
        plot.setSimpleLabels(true);

        // Convertir gráfica a imagen y añadir al PDF
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