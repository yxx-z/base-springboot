package com.yxx.business.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yxx
 * @since 2022-11-12 03:42
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRes {
    private String token;
}
