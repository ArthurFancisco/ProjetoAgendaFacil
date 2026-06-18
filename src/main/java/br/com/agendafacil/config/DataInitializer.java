package br.com.agendafacil.config;

import br.com.agendafacil.entity.*;
import br.com.agendafacil.enums.UserRole;
import br.com.agendafacil.repository.*;
import br.com.agendafacil.util.SlugUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    @Transactional
    CommandLineRunner seed(EstablishmentRepository establishmentRepository,
                           UserRepository userRepository,
                           ServiceOptionRepository serviceOptionRepository,
                           ProfessionalRepository professionalRepository,
                           BusinessHourRepository businessHourRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (establishmentRepository.count() > 0) return;

            Establishment establishment = new Establishment();
            establishment.setName("Barbearia Modelo");
            establishment.setSlug(SlugUtil.from(establishment.getName()));
            establishment.setDescription("Página de demonstração do AgendaFácil Pro. Use como base para testar agendamentos com segurança.");
            establishment.setPhone("(17) 99999-9999");
            establishment.setCity("Fernandópolis-SP");
            establishment.setAddress("Rua Exemplo, 100");
            establishment.setInstagram("@barbeariamodelo");
            establishmentRepository.save(establishment);

            AppUser owner = new AppUser();
            owner.setName("Dono da Barbearia");
            owner.setEmail("dono@agendafacil.local");
            owner.setPasswordHash(passwordEncoder.encode("Admin@123"));
            owner.setRole(UserRole.DONO);
            owner.setEstablishment(establishment);
            userRepository.save(owner);

            ServiceOption corte = service("Corte masculino", "Corte simples com acabamento", new BigDecimal("35.00"), 30, 5, false, establishment);
            ServiceOption barba = service("Barba", "Modelagem e acabamento de barba", new BigDecimal("25.00"), 20, 5, false, establishment);
            ServiceOption combo = service("Corte + barba", "Pacote completo", new BigDecimal("55.00"), 50, 10, false, establishment);
            ServiceOption progressiva = service("Progressiva", "Serviço longo com aprovação manual", new BigDecimal("120.00"), 180, 20, true, establishment);
            serviceOptionRepository.saveAll(List.of(corte, barba, combo, progressiva));

            Professional joao = professional("João", "(17) 98888-1111", establishment, List.of(corte, barba, combo));
            Professional carlos = professional("Carlos", "(17) 98888-2222", establishment, List.of(corte, barba, combo, progressiva));
            professionalRepository.saveAll(List.of(joao, carlos));

            for (int day = 1; day <= 6; day++) {
                BusinessHour hour = new BusinessHour();
                hour.setEstablishment(establishment);
                hour.setDayOfWeek(day);
                hour.setOpenTime(LocalTime.of(8, 0));
                hour.setCloseTime(day == 6 ? LocalTime.of(13, 0) : LocalTime.of(18, 0));
                businessHourRepository.save(hour);
            }
        };
    }

    private static ServiceOption service(String name, String description, BigDecimal price, int duration, int buffer, boolean approval, Establishment establishment) {
        ServiceOption s = new ServiceOption();
        s.setName(name);
        s.setDescription(description);
        s.setPrice(price);
        s.setDurationMinutes(duration);
        s.setBufferMinutes(buffer);
        s.setRequiresManualApproval(approval);
        s.setEstablishment(establishment);
        return s;
    }

    private static Professional professional(String name, String phone, Establishment establishment, List<ServiceOption> services) {
        Professional p = new Professional();
        p.setName(name);
        p.setPhone(phone);
        p.setEstablishment(establishment);
        p.getServices().addAll(services);
        return p;
    }
}
