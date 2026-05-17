package com.raktakk.backend.dto;

import java.util.List;

public record VendorDetailDTO(
        VendorDTO vendor,
        java.util.List<ServiceDTO> services
) {
}
