package org.example.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class BigObject implements Serializable {

    private Long id;
    private String name;
    private byte[] content;

}
