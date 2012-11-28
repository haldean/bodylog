package bodylog

import (
    "appengine"
    "appengine/datastore"
    "encoding/json"
    "fmt"
    "net/http"
    "strconv"
    "time"
)

type LogEntry struct {
  Device string
  Lat float64
  Lon float64
  Alt float64
  Time time.Time
}

func init() {
  http.HandleFunc("/", indexHandler)
  http.HandleFunc("/ping", logHandler)
  http.HandleFunc("/view", viewHandler)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
  fmt.Fprint(w, "Body: logged.")
}

func logHandler(w http.ResponseWriter, r *http.Request) {
  ctx := appengine.NewContext(r)

  var l LogEntry
  var err error

  l.Device = r.FormValue("id")
  if l.Device == "" {
    fmt.Fprint(w, "Must specify a device ID.\n")
    return
  }

  if l.Lat, err = strconv.ParseFloat(r.FormValue("lat"), 64); err != nil {
    fmt.Fprintf(w, "Latitude '%v' is not a float.\n", r.FormValue("lat"))
    return
  }

  if l.Lon, err = strconv.ParseFloat(r.FormValue("lon"), 64); err != nil {
    fmt.Fprintf(w, "Longitude '%v' is not a float.\n", r.FormValue("lon"))
    return
  }

  if l.Alt, err = strconv.ParseFloat(r.FormValue("alt"), 64); err != nil {
    fmt.Fprintf(w, "Altitude '%v' is not a float.\n", r.FormValue("alt"))
    return
  }

  if ts, err := strconv.ParseInt(r.FormValue("time"), 10, 64); err != nil {
    l.Time = time.Now()
  } else {
    l.Time = time.Unix(ts, 0)
  }

  ctx.Infof("Got log entry: %v", l)

  _, err = datastore.Put(
      ctx, datastore.NewIncompleteKey(ctx, "LogEntry", nil), &l)

  fmt.Fprint(w, "OK")
}

func viewHandler(w http.ResponseWriter, r *http.Request) {
  ctx := appengine.NewContext(r)
  q := datastore.
      NewQuery("LogEntry").
      Filter("Device =", r.FormValue("id")).
      Order("-Time").
      Limit(100)

  entries := make([]LogEntry, 0, 100)
  if _, err := q.GetAll(ctx, &entries); err != nil {
    http.Error(w, err.Error(), http.StatusInternalServerError)
    return
  }

  if len(entries) == 0 {
    fmt.Fprint(w, "No log entries found for this device.")
  }

  j, err := json.MarshalIndent(entries, "", "  ")
  if err != nil {
    fmt.Fprint(w, "Unable to encode entries as JSON.")
  }
  w.Write(j)
}
