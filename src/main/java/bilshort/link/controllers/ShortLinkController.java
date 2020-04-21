package bilshort.link.controllers;

import bilshort.link.models.Link;
import bilshort.link.models.LinkDTO;
import bilshort.link.services.LinkService;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequestMapping("api/v1/shortURL")
@RestController
public class ShortLinkController {

    @Autowired
    private LinkService linkService;

/*
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    boolean isUser  = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("USER"));
    boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ADMIN"));
*/

    private String generateRandomCode() {
        for (int i = 0; i < 10; i++) {
            String randomCode = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6);

            if (linkService.getLinkByCode(randomCode) == null) {
                return randomCode;
            }
        }

        return null;
    }

    @PostMapping
    public ResponseEntity<?> createShortLink(@RequestBody @NonNull LinkDTO linkDTO) {
        UrlValidator urlValidator = new UrlValidator();

        String url = linkDTO.getUrl();

        if (url.length() < 8) {
            url = "http://" + url;
        }
        else if (!url.substring(0, 8).equals("https://") && !url.substring(0, 7).equals("http://")) {
            url = "http://" + url;
        }

        linkDTO.setUrl(url);

        if (!urlValidator.isValid(linkDTO.getUrl())) {
            return ResponseEntity.badRequest().body("Given URL is invalid!");
        }

        if (linkDTO.getCode() == null) { // Random
            String randomCode = generateRandomCode();

            if (randomCode == null) {
                return ResponseEntity.badRequest().body("Unexpected Error!");
            }
            else {
                linkDTO.setCode(randomCode);
            }
        }
        else { // Custom
            if (linkService.getLinkByCode(linkDTO.getCode()) == null) {
                linkDTO.setCode(linkDTO.getCode());
            }
            else {
                return ResponseEntity.badRequest().body("Custom URL already exists!");
            }
        }

        linkDTO.setUserName(SecurityContextHolder.getContext().getAuthentication().getName());
        Link link = linkService.createShortLink(linkDTO);

        LinkDTO response = new LinkDTO();
        response.setExpTime(link.getExpTime());
        response.setUrl(link.getUrl());
        response.setCode(link.getCode());
        response.setLinkId(link.getLinkId());
        response.setDescription(link.getDescription());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllShortURLs(@RequestParam Map<String, String> params) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            return ResponseEntity.badRequest().body("You don't have authorization for this operation.");
        }

        List<LinkDTO> links = new ArrayList<>();
        if (params.isEmpty())  {

            for (Link link : linkService.getAllLinks()) {
                LinkDTO tempLink = new LinkDTO();

                tempLink.setExpTime(link.getExpTime());
                tempLink.setUrl(link.getUrl());
                tempLink.setCode(link.getCode());
                tempLink.setLinkId(link.getLinkId());
                tempLink.setUserName(link.getOwner().getUserName());
                tempLink.setDescription(link.getDescription());

                links.add(tempLink);
            }
        }
        else {
            if (params.containsKey("userId")) {

                Integer userId = Integer.parseInt(params.get("userId"));
                List<Link> linksByUserId = linkService.getLinksByUserId(userId);

                for (Link link : linksByUserId) {
                    LinkDTO tempLink = new LinkDTO();

                    tempLink.setExpTime(link.getExpTime());
                    tempLink.setUrl(link.getUrl());
                    tempLink.setCode(link.getCode());
                    tempLink.setLinkId(link.getLinkId());
                    tempLink.setUserName(link.getOwner().getUserName());
                    tempLink.setDescription(link.getDescription());

                    links.add(tempLink);
                }
            }
        }

        return ResponseEntity.ok(links);
    }

    @GetMapping(path = "{id}")
    public ResponseEntity<?> getShortURLById(@PathVariable("id") Integer id) {

        boolean isUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("USER"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ADMIN"));

        if (!isAdmin && !isUser) {
            return ResponseEntity.badRequest().body("You don't have authorization for this operation.");
        }

        Link link = linkService.getLinkById(id);

        LinkDTO response = new LinkDTO();

        response.setExpTime(link.getExpTime());
        response.setUrl(link.getUrl());
        response.setCode(link.getCode());
        response.setLinkId(link.getLinkId());
        response.setUserName(link.getOwner().getUserName());
        response.setDescription(link.getDescription());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(path = "{id}")
    public ResponseEntity<?> deleteShortURLById(@PathVariable("id") int id) {
        boolean isUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("USER"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ADMIN"));

        if (!isAdmin && !isUser) {
            return ResponseEntity.badRequest().body("You don't have authorization for this operation.");
        }

        Long operationCode = linkService.deleteLinkById(id);

        if (operationCode == 0) {
            return ResponseEntity.notFound().build();
        }
        else if (operationCode == 1) {
            return ResponseEntity.ok().body("Deletion Successful");
        }

        return ResponseEntity.badRequest().body("Unexpected Error");

    }

    @PutMapping(path = "{id}")
    public ResponseEntity<?> updateShortURLById(@PathVariable("id") int id, @RequestBody @NonNull LinkDTO linkDTO) {

        boolean isUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("USER"));
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ADMIN"));

        if (!isAdmin && !isUser) {
            return ResponseEntity.badRequest().body("You don't have authorization for this operation.");
        }

        Link link = linkService.getLinkById(id);
        link.setCode(linkDTO.getCode());
        link.setExpTime(linkDTO.getExpTime());
        link.setUrl(linkDTO.getUrl());

        link = linkService.updateLink(link);

        LinkDTO response = new LinkDTO();

        response.setExpTime(link.getExpTime());
        response.setUrl(link.getUrl());
        response.setCode(link.getCode());
        response.setLinkId(link.getLinkId());
        response.setUserName(link.getOwner().getUserName());
        response.setDescription(link.getDescription());

        return ResponseEntity.ok(response);
    }
}
