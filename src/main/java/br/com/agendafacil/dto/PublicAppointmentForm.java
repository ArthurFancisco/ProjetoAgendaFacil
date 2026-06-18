package br.com.agendafacil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class PublicAppointmentForm {
    @NotNull
    private UUID serviceId;

    @NotNull
    private UUID professionalId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime startTime;

    @NotBlank
    @Size(min = 3, max = 80)
    private String clientName;

    @NotBlank
    @Size(min = 10, max = 40)
    private String clientPhone;

    @Size(max = 255)
    private String observation;

    // Honeypot: humanos não veem este campo. Bot costuma preencher.
    private String website;

    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public UUID getProfessionalId() { return professionalId; }
    public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }
    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
