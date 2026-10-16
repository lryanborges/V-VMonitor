package com.vvmonitor.api.dto.request;

import jakarta.validation.constraints.Size;

/** RF10/RF17: nome opcional da versao (ex.: "Revisao com o cliente"); vazio vira so "v3". */
public record CreateVersionRequest(
        @Size(max = 100, message = "O nome da versão deve ter no máximo 100 caracteres.")
        String label
) {
}
