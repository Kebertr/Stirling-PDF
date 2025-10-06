package stirling.software.SPDF.controller.api.converters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import stirling.software.SPDF.model.api.converters.PdfToPsAndEpsRequest;
import stirling.software.common.util.ProcessExecutor;
import stirling.software.common.util.ProcessExecutor.ProcessExecutorResult;
import stirling.software.common.util.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/convert")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Convert", description = "Convert APIs")
public class ConvertPDFToPsAndEps {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/pdf/Ps")
    @Operation(
            summary = "Convert a PDF to a Ps or Eps",
            description = "This endpoint converts a PDF file to a Ps or Eps")
    public ResponseEntity<byte[]> processPdfToPsOrEps(@ModelAttribute PdfToPsAndEpsRequest request)
            throws IOException, InterruptedException {
        MultipartFile inputFile = request.getFileInput();
        String format = request.getOutputFormat().toLowerCase();
        File outFile = null;
        try {
            // run Ghostscript conversion
            outFile = convertToPsOrEps(inputFile, format);

            String application = "ps".equals(format) ? "application/postscript" : "application/eps";

            String outName =
                    inputFile.getOriginalFilename().replaceFirst("[.][^.]+$", "")
                            + "_converted."
                            + format;

            // Making the download
            byte[] bytes = Files.readAllBytes(outFile.toPath());
            return WebResponseUtils.bytesToWebResponse(
                    bytes, outName, MediaType.parseMediaType(application));

        } finally {
            if (outFile != null) {
                FileUtils.deleteQuietly(outFile);
            }
        }
    }

    public File convertToPsOrEps(MultipartFile inputFile, String format)
            throws IOException, InterruptedException {

        if (!"ps".equalsIgnoreCase(format) && !"eps".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Invalid format: must be 'ps' or 'eps'");
        }
        // We need a location for Ghostscript to read from
        String baseName = "input";
        Path workDir = Files.createTempDirectory("pdf2" + format);
        Path inputPath = workDir.resolve(baseName + ".pdf");
        Path outputPath = workDir.resolve(baseName + "." + format);
        Files.copy(inputFile.getInputStream(), inputPath, StandardCopyOption.REPLACE_EXISTING);

        String device = "eps".equalsIgnoreCase(format) ? "eps2write" : "ps2write";
        try {
            List<String> command = new ArrayList<>();
            command.add("gs");
            command.add("-sDEVICE=" + device);
            command.add("-dNOPAUSE");
            command.add("-dBATCH");
            command.add("-dSAFER");
            command.add("-o");
            command.add(outputPath.toString());
            command.add(inputPath.toString());
            // Runs the ghostscript command
            ProcessExecutorResult result =
                    ProcessExecutor.getInstance(ProcessExecutor.Processes.GHOSTSCRIPT)
                            .runCommandWithOutputHandling(command);

            // Check success
            if (result == null || !Files.exists(outputPath)) {
                throw new IllegalStateException("Ghostscript conversion failed");
            }
            return outputPath.toFile();
        } finally {
            // Clean up the temporary files
            try {
                Files.deleteIfExists(inputPath);
            } catch (IOException e) {
                log.warn("Failed to delete temp input file: {}", inputPath, e);
            }
        }
    }
}
