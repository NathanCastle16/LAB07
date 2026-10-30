package com.example.lab7.controller;

import com.example.lab7.entity.Customer;
import com.example.lab7.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers")
    public String listar(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        return "customer/list";
    }

    @GetMapping("/customers/new")
    public String nuevo(@ModelAttribute("customer") Customer customer) {
        return "customer/form";
    }

    @GetMapping("/customers/edit")
    public String editar(@RequestParam("id") Integer id, Model model) {
        Optional<Customer> customerOptional = customerRepository.findById(id);
        if (customerOptional.isPresent()) {
            model.addAttribute("customer", customerOptional.get());
            return "customer/form";
        }
        return "redirect:/customers";
    }

    @PostMapping("/customers/save")
    public String guardar(@ModelAttribute("customer") @Valid Customer customer,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes attr) {

        if (customer.getDocumentType() != null &&
                !customer.getDocumentType().equals("DNI") &&
                !customer.getDocumentType().equals("RUC")) {
            model.addAttribute("documentTypeError", "El tipo de documento debe ser DNI o RUC");
        }

        if (customer.getDocument() != null && customer.getDocumentType() != null) {
            if (customer.getDocumentType().equals("DNI") &&
                    !customer.getDocument().matches("[0-9]{8}")) {
                model.addAttribute("documentError", "El DNI debe contener exactamente 8 dígitos");
            }
            if (customer.getDocumentType().equals("RUC") &&
                    !customer.getDocument().matches("[0-9]{11}")) {
                model.addAttribute("documentError", "El RUC debe contener exactamente 11 dígitos");
            }
        }

        Customer existente = null;
        if (customer.getDocument() != null && !customer.getDocument().isBlank()) {
            existente = customerRepository.findByDocument(customer.getDocument());
        }
        if (existente != null &&
                (customer.getId() == null || !existente.getId().equals(customer.getId()))) {
            model.addAttribute("documentError", "El número de documento ya está registrado");
        }

        if (bindingResult.hasErrors() ||
                model.getAttribute("documentTypeError") != null ||
                model.getAttribute("documentError") != null) {
            return "customer/form";
        }

        boolean nuevo = customer.getId() == null;
        customerRepository.save(customer);
        if (nuevo) {
            attr.addFlashAttribute("msg", "Cliente registrado correctamente");
        } else {
            attr.addFlashAttribute("msg", "Cliente actualizado correctamente");
        }
        return "redirect:/customers";
    }

    @GetMapping("/customers/delete")
    public String eliminar(@RequestParam("id") Integer id, RedirectAttributes attr) {
        customerRepository.deleteById(id);
        attr.addFlashAttribute("msg", "Cliente eliminado correctamente");
        return "redirect:/customers";
    }
}
