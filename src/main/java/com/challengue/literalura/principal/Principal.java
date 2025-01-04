package com.challengue.literalura.principal;

import com.challengue.literalura.model.Autor;
import com.challengue.literalura.model.Datos;
import com.challengue.literalura.model.DatosLibro;
import com.challengue.literalura.model.Libro;
import com.challengue.literalura.repository.LiteraturaRepository;
import com.challengue.literalura.service.ConsumoAPI;
import com.challengue.literalura.service.ConvierteDatos;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private String URL_BASE = "https://gutendex.com/books/";
    private LiteraturaRepository repository;

    public Principal(LiteraturaRepository repository) {
        this.repository = repository;
    }

    public void mostrarMenu() {
        var opcion = -1;
        var menu = """
            ***** 📚 Bienvenido(a) a mi APP Literalura 📚 *****
            ********************************************
                         📑 MENU PRINCIPAL 📑
            ********************************************
            1 - Buscar Libros por TÍtulo
            2 - Listar Libros Registrados
            3 - Listar Autores Registrados
            4 - Listar Autores vivos en determinado año
            5 - Listar Libros por Idioma
            
            ********************************************
            0 - SALIR DEL PROGRAMA
            ********************************************
            Elija una opción:
            """;

        while (opcion != 0) {
            System.out.println(menu);
            try {
                opcion = Integer.valueOf(teclado.nextLine());
                switch (opcion) {
                    case 1:
                        buscarLibroPorTitulo();
                        break;
                    case 2:
                        listarLibrosRegistrados();
                        break;
                    case 3:
                        listarAutoresRegistrados();
                        break;
                    case 4:
                        listarAutoresVivos();
                        break;
                    case 5:
                        //listarLibrosPorIdioma();
                        break;
                    case 0:
                        System.out.println("Cerrando la App Literalura \uD83D\uDCD3 ...");
                        break;
                    default:
                        System.out.println("Opción no válida!");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida: " + e.getMessage());

            }
        }
    }
    public void buscarLibroPorTitulo() {
        System.out.println("""
            ********************************
             📔 BUSCAR LIBROS POR TÍTULO 📔
            ********************************
             """);
        System.out.println("Introduzca el nombre del libro a buscar:");
        var nombre = teclado.nextLine();
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombre.replace(" ", "+").toLowerCase());

        // Check if JSON is empty
        if (json.isEmpty() || !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]")) {
            var datos = conversor.obtenerDatos(json, Datos.class);

            // Process valid data
            Optional<DatosLibro> libroBuscado = datos.libros().stream()
                    .findFirst();
            if (libroBuscado.isPresent()) {
                System.out.println(
                        "\n------------- LIBRO \uD83D\uDCD9  --------------" +
                                "\nTítulo: " + libroBuscado.get().titulo() +
                                "\nAutor: " + libroBuscado.get().autores().stream()
                                .map(a -> a.nombre()).limit(1).collect(Collectors.joining()) +
                                "\nIdioma: " + libroBuscado.get().idiomas().stream().collect(Collectors.joining()) +
                                "\nNúmero de descargas: " + libroBuscado.get().descargas() +
                                "\n--------------------------------------\n"
                );

                try {
                    List<Libro> libroEncontrado = libroBuscado.stream().map(a -> new Libro(a)).collect(Collectors.toList());
                    Autor autorAPI = libroBuscado.stream().
                            flatMap(l -> l.autores().stream()
                                    .map(a -> new Autor(a)))
                            .collect(Collectors.toList()).stream().findFirst().get();
                    Optional<Autor> autorBD = repository.buscarAutorPorNombre(libroBuscado.get().autores().stream()
                            .map(a -> a.nombre())
                            .collect(Collectors.joining()));
                    Optional<Libro> libroOptional = repository.buscarLibroPorNombre(nombre);
                    if (libroOptional.isPresent()) {
                        System.out.println("El libro ya está guardado en la BD.");
                    } else {
                        Autor autor;
                        if (autorBD.isPresent()) {
                            autor = autorBD.get();
                            System.out.println("EL autor ya esta guardado en la BD");
                        } else {
                            autor = autorAPI;
                            repository.save(autor);
                        }
                        autor.setLibros(libroEncontrado);
                        repository.save(autor);
                    }
                } catch (Exception e) {
                    System.out.println("Warning! " + e.getMessage());
                }
            } else {
                System.out.println("Libro no encontrado!");
            }
        }
    }

    public void listarLibrosRegistrados () {
        System.out.println("""
                    **********************************
                     📕 LISTAR LIBROS REGISTRADOS 📕
                    **********************************
                     """);
        List<Libro> libros = repository.buscarTodosLosLibros();
        libros.forEach(l -> System.out.println(
                "*****************  LIBRO  ****************" +
                        "\nTítulo: " + l.getTitulo() +
                        "\nAutor: " + l.getAutor().getNombre() +
                        "\nIdioma: " + l.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + l.getDescargas() +
                        "\n----------------------------------------\n"
        ));
    }

    public void listarAutoresRegistrados () {
        System.out.println("""
                    **********************************
                      LISTAR AUTORES REGISTRADOS  
                    **********************************
                     """);
        List<Autor> autores = repository.findAll();
        System.out.println();
        autores.forEach(l -> System.out.println(
                "Autor: " + l.getNombre() +
                        "\nFecha de Nacimiento: " + l.getNacimiento() +
                        "\nFecha de Fallecimiento: " + l.getFallecimiento() +
                        "\nLibros: " + l.getLibros().stream()
                        .map(t -> t.getTitulo()).collect(Collectors.toList()) + "\n"
        ));
    }

    public void listarAutoresVivos () {
        System.out.println("""
                    *****************************
                       LISTAR AUTORES VIVOS 
                    *****************************
                     """);
        System.out.println("Introduzca un año para verificar el autor(es) que desea buscar:");
        try {
            var fecha = Integer.valueOf(teclado.nextLine());
            List<Autor> autores = repository.buscarAutoresVivos(fecha);
            if (!autores.isEmpty()) {
                System.out.println();
                autores.forEach(a -> System.out.println(
                        "Autor: " + a.getNombre() +
                                "\nFecha de Nacimiento: " + a.getNacimiento() +
                                "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                "\nLibros: " + a.getLibros().stream()
                                .map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                ));
            } else {
                System.out.println("No hay autores vivos en el año registrado");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ingresa un año válido " + e.getMessage());
        }
    }
}
