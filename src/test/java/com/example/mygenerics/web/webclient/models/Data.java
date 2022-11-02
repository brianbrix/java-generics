package com.example.mygenerics.web.webclient.models;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Data {
    private int id;
    private String email;
    private String first_name;
    private String last_name;
    private String avatar;
}
