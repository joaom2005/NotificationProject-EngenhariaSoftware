package pt.ipportalegre.estgd.ei.es.projectnotification.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "recipients")
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String address;


    public Recipient() {
    }


    public Recipient(String address) {
        this.address = address;
    }


    public Long getId() {
        return id;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}