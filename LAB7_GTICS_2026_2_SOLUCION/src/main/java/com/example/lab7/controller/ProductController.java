package com.example.lab7.controller;

import com.example.lab7.entity.Product;
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

import java.util.Optional;

@Controller
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products")
    public String listar(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "product/list";
    }

    @GetMapping("/products/new")
    public String nuevo(@ModelAttribute("product") Product product) {
        return "product/form";
    }

    @GetMapping("/products/edit")
    public String editar(@RequestParam("id") Integer id, Model model) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            model.addAttribute("product", productOptional.get());
            return "product/form";
        }
        return "redirect:/products";
    }

    @PostMapping("/products/save")
    public String guardar(@ModelAttribute("product") @Valid Product product,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes attr) {

        Product existente = null;
        if (product.getName() != null && !product.getName().isBlank()) {
            existente = productRepository.findByNameIgnoreCase(product.getName());
        }
        if (existente != null &&
                (product.getId() == null || !existente.getId().equals(product.getId()))) {
            model.addAttribute("nameError", "Ya existe un producto con ese nombre");
        }

        if (bindingResult.hasErrors() || model.getAttribute("nameError") != null) {
            return "product/form";
        }

        boolean nuevo = product.getId() == null;
        productRepository.save(product);
        if (nuevo) {
            attr.addFlashAttribute("msg", "Producto registrado correctamente");
        } else {
            attr.addFlashAttribute("msg", "Producto actualizado correctamente");
        }
        return "redirect:/products";
    }

    @GetMapping("/products/delete")
    public String eliminar(@RequestParam("id") Integer id, RedirectAttributes attr) {
        productRepository.deleteById(id);
        attr.addFlashAttribute("msg", "Producto eliminado correctamente");
        return "redirect:/products";
    }

    // Alternativa implementada para los errores de conversión de price y stock:
    // messages.properties (typeMismatch.product.price y typeMismatch.product.stock).
}
