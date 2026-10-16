package com.finsight.finsight_api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmailRequest {
    @NotBlank(message = "New email cannot be empty.")
    @Email(message = "The email format is invalid.")
    private String newEmail;

    @NotBlank(message = "You must enter your current password to confirm.")
    private String currentPassword;
}