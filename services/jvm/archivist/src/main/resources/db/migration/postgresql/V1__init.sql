--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA zorroa;

--
-- Name: after_task_insert(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION zorroa.after_task_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  UPDATE job_count SET int_task_total_count=int_task_total_count+1,
    int_task_state_0=int_task_state_0+1 WHERE pk_job=new.pk_job;
  RETURN NEW;
END
$$;

--
-- Name: after_task_state_change(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION zorroa.after_task_state_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $_$
DECLARE
    old_state_col VARCHAR;
    new_state_col VARCHAR;
    tx_time BIGINT;
BEGIN
  SELECT EXTRACT(EPOCH FROM NOW()) * 1000 INTO tx_time;
  old_state_col := 'int_task_state_' || old.int_state::text;
  new_state_col := 'int_task_state_' || new.int_state::text;
  EXECUTE 'UPDATE job_count SET ' || old_state_col || '=' || old_state_col || '-1,'
    || new_state_col || '=' || new_state_col || '+1, time_updated=$1 WHERE job_count.pk_job = $2' USING tx_time, new.pk_job;
  RETURN NEW;
END
$_$;

--
-- Name: bitand(integer, integer); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION zorroa.bitand(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
BEGIN
  RETURN $1 & $2;
END;
$_$;

--
-- Name: current_time_millis(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION zorroa.current_time_millis() RETURNS bigint
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN (extract(epoch from clock_timestamp()) * 1000)::BIGINT;
end;
$$;

--
-- Name: trigger_update_job_state(); Type: FUNCTION; Schema: zorroa; Owner: zorroa
--

CREATE FUNCTION zorroa.trigger_update_job_state() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  IF NEW.int_task_state_success_count + NEW.int_task_state_failure_count + NEW.int_task_state_skipped_count = NEW.int_task_total_count THEN
    UPDATE job set int_state=2 where pk_job=NEW.pk_job AND int_state=0;
  ELSE
    UPDATE job set int_state=0 where pk_job=NEW.pk_job AND int_state=2;
  END IF;

  RETURN NEW;
END
$$;


ALTER FUNCTION zorroa.trigger_update_job_state() OWNER TO zorroa;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: analyst;
--

CREATE TABLE zorroa.analyst (
    pk_analyst uuid PRIMARY KEY,
    pk_task uuid,
    int_state smallint DEFAULT 0 NOT NULL,
    int_lock_state smallint DEFAULT 0 NOT NULL,
    time_created bigint NOT NULL,
    time_ping bigint NOT NULL,
    str_endpoint text NOT NULL,
    int_total_ram integer NOT NULL,
    int_free_ram integer NOT NULL,
    flt_load double precision NOT NULL,
    int_free_disk integer DEFAULT 1024 NOT NULL,
    str_version text DEFAULT '0.41.0-1547158402'::text NOT NULL
);


CREATE INDEX analyst_pk_task_idx ON zorroa.analyst USING btree (pk_task);


--
-- Name: analyst_str_endpoint_idx; Type: INDEX; Schema: zorroa; Owner: zorroa
--

CREATE UNIQUE INDEX analyst_str_endpoint_idx ON zorroa.analyst USING btree (str_endpoint);


--
-- Name: asset;
--

CREATE TABLE zorroa.asset (
    pk_asset uuid PRIMARY KEY,
    project_id uuid NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    json_document jsonb NOT NULL,
    int_update_count bigint DEFAULT 1 NOT NULL
);

CREATE INDEX asset_pk_time_modified_idx ON zorroa.asset USING btree (time_modified);


--
-- Name: index_route;
--

CREATE TABLE zorroa.index_route (
    pk_index_route uuid PRIMARY KEY,
    project_id uuid NOT NULL,
    str_url text NOT NULL,
    str_index text NOT NULL,
    str_mapping_type text NOT NULL,
    int_mapping_major_ver smallint NOT NULL,
    int_mapping_minor_ver integer NOT NULL,
    int_replicas smallint NOT NULL,
    int_shards smallint NOT NULL,
    bool_closed boolean NOT NULL,
    bool_default_pool boolean NOT NULL,
    bool_use_rkey boolean NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    int_mapping_error_ver integer DEFAULT '-1'::integer NOT NULL
);

CREATE UNIQUE INDEX index_route_idx_uniq ON zorroa.index_route USING btree (str_url, str_index);

--
-- Name: job;
--

CREATE TABLE zorroa.job (
    pk_job uuid PRIMARY KEY,
    project_id uuid NOT NULL,
    str_name text NOT NULL,
    int_type smallint DEFAULT 0 NOT NULL,
    int_state smallint DEFAULT 0 NOT NULL,
    time_started bigint NOT NULL,
    json_args text DEFAULT '{}'::text NOT NULL,
    json_env text DEFAULT '{}'::text NOT NULL,
    int_priority smallint DEFAULT 0 NOT NULL,
    time_created bigint DEFAULT '1536693258000'::bigint NOT NULL,
    time_modified bigint DEFAULT '1536693258000'::bigint NOT NULL,
    pk_user_modified uuid NOT NULL,
    time_pause_expired bigint DEFAULT '-1'::integer NOT NULL,
    bool_paused boolean DEFAULT false NOT NULL
);

CREATE INDEX job_int_priority_idx ON zorroa.job USING btree (int_priority);
CREATE INDEX job_name_idx ON zorroa.job USING btree (str_name);
CREATE INDEX job_state_idx ON zorroa.job USING btree (int_state);
CREATE INDEX job_time_created_idx ON zorroa.job USING btree (time_created);
CREATE INDEX job_time_started_idx ON zorroa.job USING btree (time_started);
CREATE INDEX job_type_idx ON zorroa.job USING btree (int_type);

--
-- Name: job_count;

CREATE TABLE zorroa.job_count (
    pk_job uuid PRIMARY KEY,
    int_task_total_count integer DEFAULT 0 NOT NULL,
    int_task_completed_count integer DEFAULT 0 NOT NULL,
    int_task_state_0 integer DEFAULT 0 NOT NULL,
    int_task_state_5 integer DEFAULT 0 NOT NULL,
    int_task_state_1 integer DEFAULT 0 NOT NULL,
    int_task_state_2 integer DEFAULT 0 NOT NULL,
    int_task_state_3 integer DEFAULT 0 NOT NULL,
    int_task_state_4 integer DEFAULT 0 NOT NULL,
    time_updated bigint DEFAULT zorroa.current_time_millis() NOT NULL,
    int_max_running_tasks integer DEFAULT 1024 NOT NULL,
    CONSTRAINT job_count_max_running_check CHECK (((int_task_state_1 + int_task_state_5) <= int_max_running_tasks))
);

CREATE INDEX job_count_int_task_state_0_idx ON zorroa.job_count USING btree (int_task_state_0);


--
-- Name: job_stat;
--

CREATE TABLE zorroa.job_stat (
    pk_job uuid PRIMARY KEY REFERENCES job(pk_job) ON DELETE CASCADE,
    int_asset_total_count integer DEFAULT 0 NOT NULL,
    int_asset_create_count integer DEFAULT 0 NOT NULL,
    int_asset_error_count integer DEFAULT 0 NOT NULL,
    int_asset_warning_count integer DEFAULT 0 NOT NULL,
    int_asset_update_count integer DEFAULT 0 NOT NULL,
    int_asset_replace_count integer DEFAULT 0 NOT NULL
);


CREATE TABLE zorroa.pipeline (
    pk_pipeline uuid PRIMARY KEY,
    project_id uuid NOT NULL,
    int_type smallint NOT NULL,
    str_name text NOT NULL,
    str_description text NOT NULL,
    bool_standard boolean DEFAULT false NOT NULL,
    json_processors text DEFAULT '[]'::text NOT NULL,
    int_version integer DEFAULT 1 NOT NULL,
    time_created bigint DEFAULT '1536693258000'::bigint NOT NULL,
    time_modified bigint DEFAULT '1536693258000'::bigint NOT NULL
);

CREATE UNIQUE INDEX pipeline_name_uidx ON zorroa.pipeline USING btree (project_id, str_name);


--
-- Name: plugin;
--

CREATE TABLE zorroa.plugin (
    pk_plugin uuid PRIMARY KEY,
    str_name text NOT NULL,
    str_lang text NOT NULL,
    str_description text NOT NULL,
    str_version text NOT NULL,
    str_publisher text NOT NULL,
    time_created bigint NOT NULL,
    time_modified bigint NOT NULL,
    str_md5 text
);

CREATE UNIQUE INDEX plugin_str_name_idx ON zorroa.plugin USING btree (str_name);

--
-- Name: processor;
--

CREATE TABLE zorroa.processor (
    pk_processor uuid PRIMARY KEY,
    project_id uuid,
    str_name text NOT NULL,
    str_file text NOT NULL,
    str_type text NOT NULL,
    str_description text DEFAULT ''::text NOT NULL,
    time_updated bigint NOT NULL,
    json_display jsonb DEFAULT '[]'::jsonb NOT NULL,
    list_file_types text[] DEFAULT '{}'::text[] NOT NULL,
    fti_keywords tsvector NOT NULL
);

CREATE INDEX processor_fti_keywords_idx ON zorroa.processor USING gin (fti_keywords);
CREATE UNIQUE INDEX processor_str_name_idx ON zorroa.processor USING btree (str_name);

-- Name: task; Type: TABLE; Schema: zorroa; Owner: zorroa

CREATE TABLE zorroa.task (
    pk_task uuid PRIMARY KEY,
    pk_parent uuid REFERENCES task(pk_task) ON DELETE CASCADE,
    pk_job uuid NOT NULL REFERENCES job(pk_job) ON DELETE CASCADE,
    int_state smallint DEFAULT 0 NOT NULL,
    time_started bigint DEFAULT '-1'::integer NOT NULL,
    time_stopped bigint DEFAULT '-1'::integer NOT NULL,
    time_created bigint NOT NULL,
    time_state_change bigint NOT NULL,
    time_ping bigint DEFAULT 0 NOT NULL,
    int_order smallint DEFAULT 0 NOT NULL,
    int_exit_status smallint DEFAULT '-1'::integer NOT NULL,
    str_host text,
    str_name text NOT NULL,
    time_modified bigint DEFAULT '1536693258000'::bigint NOT NULL,
    json_script text DEFAULT '{}'::text NOT NULL,
    int_run_count integer DEFAULT 0 NOT NULL
);

CREATE INDEX task_order_idx ON zorroa.task USING btree (int_order);
CREATE INDEX task_parent_idx ON zorroa.task USING btree (pk_parent);
CREATE INDEX task_pk_job_idx ON zorroa.task USING btree (pk_job);
CREATE INDEX task_state_idx ON zorroa.task USING btree (int_state);

--
-- Name: task_error; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE zorroa.task_error (
    pk_task_error uuid PRIMARY KEY,
    pk_task uuid NOT NULL REFERENCES task(pk_task) ON DELETE CASCADE,
    pk_job uuid NOT NULL REFERENCES job(pk_job) ON DELETE CASCADE,
    pk_asset uuid,
    project_id uuid NOT NULL,
    str_message text,
    str_path text,
    str_processor text,
    str_endpoint text,
    str_extension text,
    time_created bigint NOT NULL,
    bool_fatal boolean NOT NULL,
    str_phase text NOT NULL,
    json_stack_trace jsonb,
    fti_keywords tsvector NOT NULL
);

CREATE INDEX task_error_project_id_idx ON zorroa.task_error USING btree (project_id);
CREATE INDEX task_error_fti_keywords_idx ON zorroa.task_error USING gin (fti_keywords);
CREATE INDEX task_error_pk_asset_idx ON zorroa.task_error USING btree (pk_asset);
CREATE INDEX task_error_pk_job_idx ON zorroa.task_error USING btree (pk_job);
CREATE INDEX task_error_pk_task_idx ON zorroa.task_error USING btree (pk_task);
CREATE INDEX task_error_str_path_idx ON zorroa.task_error USING btree (str_path);
CREATE INDEX task_error_str_processor_idx ON zorroa.task_error USING btree (str_processor);
CREATE INDEX task_error_time_created_idx ON zorroa.task_error USING btree (time_created);

--
-- Name: task_stat; Type: TABLE; Schema: zorroa; Owner: zorroa
--

CREATE TABLE zorroa.task_stat (
    pk_task uuid NOT NULL REFERENCES task(pk_task) ON DELETE CASCADE,
    pk_job uuid NOT NULL REFERENCES job(pk_job) ON DELETE CASCADE,
    int_asset_total_count integer DEFAULT 0 NOT NULL,
    int_asset_create_count integer DEFAULT 0 NOT NULL,
    int_asset_error_count integer DEFAULT 0 NOT NULL,
    int_asset_warning_count integer DEFAULT 0 NOT NULL,
    int_asset_update_count integer DEFAULT 0 NOT NULL,
    int_asset_replace_count integer DEFAULT 0 NOT NULL
);

CREATE INDEX task_stat_pk_job_idx ON zorroa.task_stat USING btree (pk_job);


---
--- Triggers
---
CREATE TRIGGER trig_after_task_insert AFTER INSERT ON zorroa.task FOR EACH ROW EXECUTE PROCEDURE zorroa.after_task_insert();
CREATE TRIGGER trig_after_task_state_change AFTER UPDATE ON zorroa.task FOR EACH ROW WHEN ((old.int_state <> new.int_state)) EXECUTE PROCEDURE zorroa.after_task_state_change();
