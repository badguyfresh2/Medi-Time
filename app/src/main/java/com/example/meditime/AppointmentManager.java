package com.example.meditime;

import com.example.meditime.model.Appointment;
import java.util.ArrayList;
import java.util.List;

public class AppointmentManager {
    private static AppointmentManager instance;
    private List<Appointment> appointmentList;

    private AppointmentManager() {
        appointmentList = new ArrayList<>();
        // Add sample upcoming appointments (optional)
        addSampleData();
    }

    public static synchronized AppointmentManager getInstance() {
        if (instance == null) {
            instance = new AppointmentManager();
        }
        return instance;
    }

    public List<Appointment> getAppointments() {
        return appointmentList;
    }

    public void addAppointment(Appointment appointment) {
        appointmentList.add(appointment);
    }

    public void removeAppointment(Appointment appointment) {
        appointmentList.remove(appointment);
    }

    private void addSampleData() {
        // Pre-populate with a few sample appointments for demonstration
        appointmentList.add(new Appointment(
        ));
        appointmentList.add(new Appointment(
        ));
        appointmentList.add(new Appointment(
        ));

        // Mark one as checked-in (optional)
        if (appointmentList.size() > 1) {
            appointmentList.get(1).setStatus("Checked In");
        }
    }
}
