package br.com.agendafacil.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ServiceForm {
    @NotBlank
    @Size(min = 3, max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    @Min(5)
    @Max(600)
    private int durationMinutes = 30;

    @Min(0)
    @Max(120)
    private int bufferMinutes = 0;

    private boolean requiresManualApproval;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }
    public boolean isRequiresManualApproval() { return requiresManualApproval; }
    public void setRequiresManualApproval(boolean requiresManualApproval) { this.requiresManualApproval = requiresManualApproval; }
}
