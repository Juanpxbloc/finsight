package com.finsight.finsight_api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "You must enter your current password.")
    private String currentPassword;

    @NotBlank(message = "The new password cannot be empty.")
    @Size(min = 6, message = "The new password must have at least 6 characters.")
    private String newPassword;
}