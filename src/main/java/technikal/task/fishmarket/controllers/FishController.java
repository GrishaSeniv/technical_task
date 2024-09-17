package technikal.task.fishmarket.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import technikal.task.fishmarket.models.Fish;
import technikal.task.fishmarket.models.FishDto;
import technikal.task.fishmarket.services.FishRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/fish")
public class FishController {
    @Autowired
    private FishRepository repo;

    @GetMapping({"", "/"})
    public String showFishList(Model model) {
        List<Fish> fishlist = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("fishlist", fishlist);
        return "index";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String showCreatePage(Model model) {
        FishDto fishDto = new FishDto();
        model.addAttribute("fishDto", fishDto);
        return "createFish";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/delete")
    public String deleteFish(@RequestParam int id) {

        try {

            Fish fish = repo.findById(id).get();

            fish.getImageFileNames().forEach(FishController::deleteFromStorage);
            repo.delete(fish);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        return "redirect:/fish";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public String addFish(@Valid @ModelAttribute FishDto fishDto, BindingResult result) {

        if (fishDto.getImageFiles().isEmpty()) {
            result.addError(new FieldError("fishDto", "imageFiles", "Потрібне фото рибки"));
        }

        if (result.hasErrors()) {
            return "createFish";
        }

        List<MultipartFile> images = fishDto.getImageFiles();
        Date catchDate = new Date();
        long catchDateTime = catchDate.getTime();

        Map<MultipartFile, String> imageStorageFileNameMap = images.stream()
                .collect(Collectors.toMap(k -> k, image -> catchDateTime + "_" + image.getOriginalFilename()));

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            imageStorageFileNameMap.forEach((key, value) -> saveToStorage(key, value, uploadDir));

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        Fish fish = new Fish();

        fish.setCatchDate(catchDate);
        fish.setImageFileNames(imageStorageFileNameMap.values().stream().toList());
        fish.setName(fishDto.getName());
        fish.setPrice(fishDto.getPrice());

        repo.save(fish);

        return "redirect:/fish";
    }

    private static void deleteFromStorage(String imageFileName) {
        Path imagePath = Paths.get("public/images/" + imageFileName);
        try {
            Files.delete(imagePath);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    private static void saveToStorage(MultipartFile image, String storageFileName, String uploadDir) {
        try (InputStream inputStream = image.getInputStream()) {
            Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }
}
