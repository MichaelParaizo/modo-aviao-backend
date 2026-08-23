package br.com.modoaviao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_resultados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ModoFuga modo;

    @Column(nullable = false)
    private String titulo;

    private String emoji;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;
}
