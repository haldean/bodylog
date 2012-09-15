package main

import (
	"database/sql"
	"flag"
	"fmt"
	_ "github.com/mattn/go-sqlite3"
	"net/http"
	"strconv"
)

var fileName = flag.String(
	"database", "/tmp/bodylog.db", "Sqlite database used to store location data")

var db *sql.DB
var AllowedDeviceIds []string = []string{"HNUUUUU="}

func DeviceAllowed(deviceId string) bool {
	for _, id := range AllowedDeviceIds {
		if id == deviceId {
			return true
		}
	}
	return false
}

func validFloat(value string) bool {
	_, err := strconv.ParseFloat(value, 64)
	return err == nil
}

func validUint(value string) bool {
	_, err := strconv.ParseUint(value, 10, 64)
	return err == nil
}

func httpHandler(w http.ResponseWriter, r *http.Request) {
	deviceId := r.FormValue("id")
	if !DeviceAllowed(deviceId) {
		fmt.Fprintf(w, "Device ID not allowed.")
		return
	}

	var lat, lon, alt float64
	var time uint64
	var err error

	if lat, err = strconv.ParseFloat(r.FormValue("lat"), 64); err != nil {
		fmt.Fprintf(
			w, "Latitude '%v' is not a valid floating point number\n", r.FormValue("lat"))
		return
	}

	if lon, err = strconv.ParseFloat(r.FormValue("lon"), 64); err != nil {
		fmt.Fprintf(
			w, "Longitude '%v' is not a valid floating point number\n", r.FormValue("lon"))
		return
	}

	if alt, err = strconv.ParseFloat(r.FormValue("alt"), 64); err != nil {
		fmt.Fprintf(
			w, "Altitude '%v' is not a valid floating point number\n", r.FormValue("alt"))
		return
	}

	if time, err = strconv.ParseUint(r.FormValue("time"), 10, 64); err != nil {
		fmt.Fprintf(
			w, "Time '%v' is not a valid unsigned integer\n", r.FormValue("time"))
		return
	}

	fmt.Fprintf(w, "%v %v %v %v\n", lat, lon, alt, time)

	tx, err := db.Begin()
	if err != nil {
		fmt.Println(err)
		fmt.Fprintf(w, "%v\n", err)
		return
	}

	stmt, err := tx.Prepare(
		`insert into location(time, device, latitude, longitude, altitude)
     values (?, ?, ?, ?, ?)`)
	if err != nil {
		fmt.Println(err)
		fmt.Fprintf(w, "%v\n", err)
		return
	}
	defer stmt.Close()

	_, err = stmt.Exec(time, deviceId, lat, lon, alt)
	if err != nil {
		fmt.Println(err)
		fmt.Fprintf(w, "%v\n", err)
		return
	}
	tx.Commit()

	fmt.Fprintf(w, "Logged.")
}

func initDatabase() bool {
	var err error
	db, err = sql.Open("sqlite3", *fileName)
	if err != nil {
		fmt.Printf("Could not open database file: %v\n", err)
		return false
	}

	_, err = db.Exec(
		`create table if not exists location (
      time integer not null primary key,
      device text not null,
      latitude float not null,
      longitude float not null,
      altitude float not null)`)
	if err != nil {
		fmt.Printf("Could not create table: %v\n", err)
		return false
	}

	return true
}

func main() {
	flag.Parse()
	if !initDatabase() {
		db.Close()
		return
	}
	defer db.Close()

	http.HandleFunc("/", httpHandler)
	http.ListenAndServe(":8080", nil)
}
