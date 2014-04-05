# initial schema
 
# --- !Ups

CREATE TABLE config (
    confName varchar(255) NOT NULL,
    confValue varchar(255) NOT NULL,
    CONSTRAINT config_pkey PRIMARY KEY (confName)
);

CREATE SEQUENCE line_id_seq;
CREATE TABLE lines (
    id integer NOT NULL DEFAULT nextval('line_id_seq'),
    label varchar(255),
    period integer NOT NULL DEFAULT 15,
    linetype integer NOT NULL DEFAULT 0,
    CONSTRAINT lines_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE node_id_seq;
CREATE TABLE nodes (
    id integer NOT NULL DEFAULT nextval('node_id_seq'),
    label varchar(255),
    lat double precision NOT NULL DEFAULT 0,
    lng double precision NOT NULL DEFAULT 0,
    CONSTRAINT nodes_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE edge_id_seq;
CREATE TABLE edges (
	id integer NOT NULL DEFAULT nextval('edge_id_seq'),
    line integer NOT NULL,
    sourceNode integer NOT NULL,
    targetNode integer NOT NULL, 
    distance smallint NOT NULL,
    duration smallint NOT NULL DEFAULT 5,
    CONSTRAINT fk_source_node FOREIGN KEY (sourceNode)
      REFERENCES nodes (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_target_node FOREIGN KEY (targetNode)
      REFERENCES nodes (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  	CONSTRAINT fk_edge_line FOREIGN KEY (line)
      REFERENCES lines (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);
 
# --- !Downs
 
DROP TABLE lines;
DROP SEQUENCE line_id_seq;
DROP TABLE nodes;
DROP SEQUENCE node_id_seq;
DROP TABLE edges;
DROP SEQUENCE edge_id_seq;