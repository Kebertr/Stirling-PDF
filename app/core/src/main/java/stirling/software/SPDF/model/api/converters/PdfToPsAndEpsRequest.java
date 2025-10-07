package stirling.software.SPDF.model.api.converters;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import stirling.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class PdfToPsAndEpsRequest extends PDFFile {

    @Schema(
            description = "The output format (ps or eps)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"ps", "eps"})
    private String outputFormat;
}
