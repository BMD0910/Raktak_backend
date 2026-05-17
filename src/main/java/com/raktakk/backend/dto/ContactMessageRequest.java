package com.raktakk.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessageRequest {
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100)
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 3, max = 100)
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, max = 5000)
    private String message;
}
