package stirling.software.SPDF.controller.api.converters;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import stirling.software.SPDF.model.api.converters.PdfToPsAndEpsRequest;

class ConvertPDFToPsAndEpsTest {

    private ConvertPDFToPsAndEps controller;

    @BeforeEach
    void setUp() {
        controller = new ConvertPDFToPsAndEps();
    }

    @Test
    void testConvertPdfToPsEpsExist() throws IOException, InterruptedException {
        // Making the mock pdf and getting its bytes
        byte[] pdfBytes = "%PDF-1.4\n%EOF".getBytes();
        MultipartFile inputFile =
                new MockMultipartFile("fileInput", "sample.pdf", "application/pdf", pdfBytes);

        File result = controller.convertToPsOrEps(inputFile, "ps");

        assertTrue(result.exists());
        result.delete();
    }

    @Test
    void testConvertToPs() throws Exception {
        // Making a mock pdf
        byte[] pdfBytes = "%PDF-1.4\n%EOF".getBytes();
        MultipartFile inputFile =
                new MockMultipartFile("fileInput", "sample.pdf", "application/pdf", pdfBytes);

        File result = controller.convertToPsOrEps(inputFile, "ps");

        assertTrue(result.getName().endsWith(".ps"));
        result.delete();
    }

    @Test
    void testConvertToEps() throws Exception {
        byte[] pdfBytes = "%PDF-1.4\n%EOF".getBytes();
        MultipartFile inputFile =
                new MockMultipartFile("fileInput", "sample.pdf", "application/pdf", pdfBytes);

        File result = controller.convertToPsOrEps(inputFile, "eps");

        assertTrue(result.getName().endsWith(".eps"));
        result.delete();
    }

    @Test
    void testConvertToPs_InvalidFormat_Throws() {
        byte[] pdfBytes = "%PDF-1.4\n%EOF".getBytes();
        MultipartFile inputFile =
                new MockMultipartFile("fileInput", "sample.pdf", "application/pdf", pdfBytes);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    controller.convertToPsOrEps(inputFile, "jpg");
                });
    }

    @SuppressWarnings("deprecation")
    @Test
    void testProcessPdfToPsRequest() throws Exception {
        PdfToPsAndEpsRequest request = new PdfToPsAndEpsRequest();
        byte[] fakePdf = "%PDF-1.4\n%EOF".getBytes();
        MockMultipartFile mockFile =
                new MockMultipartFile("fileInput", "file.pdf", "application/pdf", fakePdf);
        request.setFileInput(mockFile);
        request.setOutputFormat("ps");

        ConvertPDFToPsAndEps mockController =
                new ConvertPDFToPsAndEps() {
                    // Instead of calling on ghostscript we simulate it
                    @Override
                    public File convertToPsOrEps(MultipartFile f, String format)
                            throws IOException {
                        File temp = File.createTempFile("fake", "." + format);
                        Files.write(temp.toPath(), "fake output".getBytes());
                        return temp;
                    }
                };

        ResponseEntity<byte[]> response = mockController.processPdfToPsOrEps(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("application/postscript", response.getHeaders().getContentType().toString());
        assertTrue(new String(response.getBody()).contains("fake output"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testProcessPdfToEpsRequest() throws Exception {
        PdfToPsAndEpsRequest req = new PdfToPsAndEpsRequest();
        byte[] fakePdf = "%PDF-1.4\n%EOF".getBytes();
        MockMultipartFile mockFile =
                new MockMultipartFile("fileInput", "file.pdf", "application/pdf", fakePdf);
        req.setFileInput(mockFile);
        req.setOutputFormat("eps");

        ConvertPDFToPsAndEps mockController =
                new ConvertPDFToPsAndEps() {
                    @Override
                    public File convertToPsOrEps(MultipartFile f, String format)
                            throws IOException {
                        File temp = File.createTempFile("fake", "." + format);
                        Files.write(temp.toPath(), "fake output".getBytes());
                        return temp;
                    }
                };

        ResponseEntity<byte[]> response = mockController.processPdfToPsOrEps(req);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("application/eps", response.getHeaders().getContentType().toString());
        assertTrue(new String(response.getBody()).contains("fake output"));
    }

    @Test
    void testProcessPdfToPs_InvalidFormat_Throws() {
        PdfToPsAndEpsRequest request = new PdfToPsAndEpsRequest();
        MockMultipartFile mockFile =
                new MockMultipartFile("fileInput", "file.pdf", "application/pdf", new byte[0]);
        request.setFileInput(mockFile);
        request.setOutputFormat("txt");
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    controller.processPdfToPsOrEps(request);
                });
    }

    @Test
    void testConvertToPsFailsGhostscript() throws Exception {
        MultipartFile inputFile =
                new MockMultipartFile("fileInput", "test.pdf", "application/pdf", new byte[0]);

        ConvertPDFToPsAndEps brokenController =
                new ConvertPDFToPsAndEps() {
                    @Override
                    public File convertToPsOrEps(MultipartFile f, String format)
                            throws IOException {
                        return new File("no_output." + format);
                    }
                };

        PdfToPsAndEpsRequest request = new PdfToPsAndEpsRequest();
        request.setFileInput(inputFile);
        request.setOutputFormat("ps");
        assertThrows(
                IOException.class,
                () -> {
                    brokenController.processPdfToPsOrEps(request);
                });
    }
}
