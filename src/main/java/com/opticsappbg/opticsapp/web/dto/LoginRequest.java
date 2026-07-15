package com.opticsappbg.opticsapp.web.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Size(min = 4, message = "Username must be at least 4 symbols")
    private String username;

    @Size(min = 4, message = "Password must be at least 4 symbols")
    private String password;
}

