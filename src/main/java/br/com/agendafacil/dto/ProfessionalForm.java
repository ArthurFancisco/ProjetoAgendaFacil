package br.com.agendafacil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProfessionalForm {
    @NotBlank
    @Size(min = 3, max = 120)
    private String name;

    @Size(max = 40)
    private String phone;

    private Set<UUID> serviceIds = new HashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Set<UUID> getServiceIds() { return serviceIds; }
    public void setServiceIds(Set<UUID> serviceIds) { this.serviceIds = serviceIds; }
}
