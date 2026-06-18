package br.com.agendafacil.service;

import br.com.agendafacil.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {
    public String normalize(String phone) {
        if (phone == null) {
            throw new BusinessException("Informe um WhatsApp válido.");
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }
        if (digits.length() < 12 || digits.length() > 13) {
            throw new BusinessException("Informe um WhatsApp com DDD. Exemplo: (17) 99999-9999.");
        }
        return digits;
    }
}
