package com.example.lab7.controller;

import com.example.lab7.entity.Customer;
import com.example.lab7.entity.Invoice;
import com.example.lab7.entity.InvoiceDetail;
import com.example.lab7.entity.Product;
import com.example.lab7.repository.CustomerRepository;
import com.example.lab7.repository.InvoiceDetailRepository;
import com.example.lab7.repository.InvoiceRepository;
import com.example.lab7.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class OrderController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderController(InvoiceRepository invoiceRepository,
                           InvoiceDetailRepository invoiceDetailRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceDetailRepository = invoiceDetailRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/orders")
    public String listar(Model model) {
        model.addAttribute("orders", invoiceRepository.findAll());
        return "order/list";
    }

    @GetMapping("/orders/new")
    public String nuevo(@ModelAttribute("invoice") Invoice invoice, Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("products", products);

        List<Boolean> selectedFlags = new ArrayList<>();
        List<String> quantityValues = new ArrayList<>();
        List<String> quantityErrors = new ArrayList<>();
        for (Product product : products) {
            selectedFlags.add(false);
            quantityValues.add("0");
            quantityErrors.add("");
        }
        model.addAttribute("selectedFlags", selectedFlags);
        model.addAttribute("quantityValues", quantityValues);
        model.addAttribute("quantityErrors", quantityErrors);
        return "order/form";
    }

    @PostMapping("/orders/save")
    public String guardar(@ModelAttribute("invoice") @Valid Invoice invoice,
                          BindingResult bindingResult,
                          @RequestParam(name = "customerId", required = false) Integer customerId,
                          @RequestParam(name = "selectedProductIds", required = false) List<Integer> selectedProductIds,
                          @RequestParam("allProductIds") List<Integer> allProductIds,
                          @RequestParam("quantities") List<String> quantities,
                          Model model,
                          RedirectAttributes attr) {

        boolean hayErrores = bindingResult.hasErrors();
        Customer customer = null;

        if (invoice.getType() != null &&
                !invoice.getType().equals("Factura") &&
                !invoice.getType().equals("Boleta")) {
            model.addAttribute("typeError", "El tipo debe ser Factura o Boleta");
            hayErrores = true;
        }

        if (invoice.getDate() == null) {
            model.addAttribute("dateError", "Ingrese la fecha");
            hayErrores = true;
        } else if (invoice.getDate().isAfter(LocalDate.now())) {
            model.addAttribute("dateError", "La fecha no puede ser futura");
            hayErrores = true;
        }

        if (customerId == null) {
            model.addAttribute("customerError", "Seleccione un cliente");
            hayErrores = true;
        } else {
            customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                model.addAttribute("customerError", "Seleccione un cliente válido");
                hayErrores = true;
            }
        }

        if (customer != null && invoice.getType() != null) {
            if (invoice.getType().equals("Factura") && !customer.getDocumentType().equals("RUC")) {
                model.addAttribute("globalError", "Factura solo puede emitirse a un cliente con RUC");
                hayErrores = true;
            }
            if (invoice.getType().equals("Boleta") && !customer.getDocumentType().equals("DNI")) {
                model.addAttribute("globalError", "Boleta solo puede emitirse a un cliente con DNI");
                hayErrores = true;
            }
        }

        if (selectedProductIds == null || selectedProductIds.isEmpty()) {
            model.addAttribute("globalError", "Debe seleccionar al menos un producto");
            hayErrores = true;
            selectedProductIds = new ArrayList<>();
        }

        List<Product> products = productRepository.findAll();
        List<String> quantityErrors = new ArrayList<>();
        List<String> quantityValues = new ArrayList<>();
        List<Boolean> selectedFlags = new ArrayList<>();
        List<Integer> cantidadesValidas = new ArrayList<>();
        List<Product> productosSeleccionados = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            quantityErrors.add("");
            quantityValues.add("0");
            selectedFlags.add(false);
        }

        for (int i = 0; i < allProductIds.size(); i++) {
            Integer productId = allProductIds.get(i);
            if (selectedProductIds.contains(productId)) {
                int posicionProducto = -1;
                for (int j = 0; j < products.size(); j++) {
                    if (products.get(j).getId().equals(productId)) {
                        posicionProducto = j;
                        break;
                    }
                }

                if (posicionProducto >= 0) {
                    selectedFlags.set(posicionProducto, true);
                    Product product = products.get(posicionProducto);
                    String cantidadTexto = "";
                    if (i < quantities.size()) {
                        cantidadTexto = quantities.get(i);
                    }
                    quantityValues.set(posicionProducto, cantidadTexto);

                    Integer cantidad = null;
                    try {
                        cantidad = Integer.parseInt(cantidadTexto);
                    } catch (Exception e) {
                        quantityErrors.set(posicionProducto, "La cantidad debe ser un número entero");
                        hayErrores = true;
                    }

                    if (cantidad != null) {
                        if (cantidad <= 0) {
                            quantityErrors.set(posicionProducto, "La cantidad debe ser mayor que cero");
                            hayErrores = true;
                        } else if (cantidad > product.getStock()) {
                            quantityErrors.set(posicionProducto, "La cantidad no puede superar el stock disponible");
                            hayErrores = true;
                        } else {
                            productosSeleccionados.add(product);
                            cantidadesValidas.add(cantidad);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < selectedProductIds.size(); i++) {
            for (int j = i + 1; j < selectedProductIds.size(); j++) {
                if (selectedProductIds.get(i).equals(selectedProductIds.get(j))) {
                    model.addAttribute("globalError", "No se puede repetir el mismo producto en un pedido");
                    hayErrores = true;
                }
            }
        }

        if (hayErrores) {
            model.addAttribute("customers", customerRepository.findAll());
            model.addAttribute("products", products);
            model.addAttribute("selectedCustomerId", customerId);
            model.addAttribute("selectedFlags", selectedFlags);
            model.addAttribute("quantityValues", quantityValues);
            model.addAttribute("quantityErrors", quantityErrors);
            return "order/form";
        }

        invoice.setCustomer(customer);
        invoiceRepository.save(invoice);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < productosSeleccionados.size(); i++) {
            Product product = productosSeleccionados.get(i);
            Integer cantidad = cantidadesValidas.get(i);
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cantidad));

            InvoiceDetail detail = new InvoiceDetail();
            detail.setInvoice(invoice);
            detail.setProduct(product);
            detail.setQuantity(cantidad);
            detail.setPrice(product.getPrice());
            detail.setSubtotal(subtotal);
            invoiceDetailRepository.save(detail);

            product.setStock(product.getStock() - cantidad);
            productRepository.save(product);

            total = total.add(subtotal);
        }

        attr.addFlashAttribute("msg", "Pedido registrado correctamente. Total: S/ " + total);
        return "redirect:/orders";
    }

    @GetMapping("/orders/view")
    public String ver(@RequestParam("id") Integer id, Model model) {
        Optional<Invoice> invoiceOptional = invoiceRepository.findById(id);
        if (invoiceOptional.isPresent()) {
            Invoice invoice = invoiceOptional.get();
            List<InvoiceDetail> details = invoiceDetailRepository.findByInvoice_Id(id);
            BigDecimal total = BigDecimal.ZERO;
            for (InvoiceDetail detail : details) {
                total = total.add(detail.getSubtotal());
            }
            model.addAttribute("invoice", invoice);
            model.addAttribute("details", details);
            model.addAttribute("total", total);
            return "order/view";
        }
        return "redirect:/orders";
    }


}
