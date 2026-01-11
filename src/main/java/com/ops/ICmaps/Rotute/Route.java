package com.ops.ICmaps.Rotute;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "route")
public class Route {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  // good default for MySQL/Postgres auto-increment
  private Long id;
  private String destinationId;
  private String destinationName;
  private String userId;
  private String name;

  protected Route() {
  }

  public Route(String userId, String destinationId, String destinationName, String name) {
    this.userId = userId;
    this.destinationName = destinationName;
    this.destinationId = destinationId;
    this.name = name;
  }

  public Long getId() {
    return this.id;
  }

  public String getDestinationId() {
    return this.destinationId;
  }

  public void setDestinationId(String destinationId) {
    this.destinationId = destinationId;
  }

  public String getDestinationName() {
    return this.destinationName;
  }

  public void setDestinationName(String destinationName) {
    this.destinationName = destinationName;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
