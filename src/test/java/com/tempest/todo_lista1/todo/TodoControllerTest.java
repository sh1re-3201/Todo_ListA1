package com.tempest.todo_lista1.todo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long createTodo(String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    @DisplayName("C1: create returns 201 with Location header and full response body")
    void c1_create_returns201WithLocationAndBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Buy groceries",
                                  "description": "Milk and eggs",
                                  "dueDate": "2026-10-20"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Buy groceries")))
                .andExpect(jsonPath("$.description", is("Milk and eggs")))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.dueDate", is("2026-10-20")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        org.hamcrest.MatcherAssert.assertThat(
                result.getResponse().getHeader("Location"),
                endsWith("/api/todos/" + id)
        );
    }

    @Test
    @DisplayName("C2: create trims title and blank description becomes null")
    void c2_create_trimsTitle_blankDescriptionBecomesNull() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Milk  ",
                                  "description": "   "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Milk")))
                .andExpect(jsonPath("$.description", nullValue()));
    }

    @Test
    @DisplayName("C3: create minimal body defaults completed to false and dueDate to null")
    void c3_create_minimalBody() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Minimal"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Minimal")))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.dueDate", nullValue()));
    }

    @Test
    @DisplayName("C4: create ignores client-supplied id, timestamps, and completed status")
    void c4_create_ignoresClientIdTimestampsAndCompleted() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 12345,
                                  "title": "Ignored fields",
                                  "createdAt": "2000-01-01T00:00:00Z",
                                  "completed": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(12345)))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.createdAt", not(endsWith("2000-01-01T00:00:00Z"))));
    }

    @Test
    @DisplayName("C5: create missing title returns 400 validation error")
    void c5_create_missingTitle_400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Validation failed")))
                .andExpect(jsonPath("$.errors.title", is("Title is required")));
    }

    @Test
    @DisplayName("C6: create blank title returns 400 validation error")
    void c6_create_blankTitle_400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.title", is("Title is required")));
    }

    @Test
    @DisplayName("C7: create title length boundary (200 ok, 201 bad request)")
    void c7_create_titleBoundary() throws Exception {
        String title200 = "a".repeat(200);
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title200 + "\"}"))
                .andExpect(status().isCreated());

        String title201 = "a".repeat(201);
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title201 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", is("Title must be at most 200 characters")));
    }

    @Test
    @DisplayName("C8: create description length boundary (1000 ok, 1001 bad request)")
    void c8_create_descriptionBoundary() throws Exception {
        String desc1000 = "d".repeat(1000);
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid\",\"description\":\"" + desc1000 + "\"}"))
                .andExpect(status().isCreated());

        String desc101 = "d".repeat(1001);
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid\",\"description\":\"" + desc101 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description", is("Description must be at most 1000 characters")));
    }

    @Test
    @DisplayName("C9: create multiple validation errors returned in errors map")
    void c9_create_multipleErrors() throws Exception {
        String desc1001 = "d".repeat(1001);
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"" + desc1001 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", notNullValue()))
                .andExpect(jsonPath("$.errors.description", notNullValue()));
    }

    @Test
    @DisplayName("C10: create malformed JSON returns 400 ProblemDetail")
    void c10_create_malformedJson_400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Malformed JSON or invalid field format (dates must be yyyy-MM-dd)")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13-45", "12/10/2026", "2026-02-30"})
    @DisplayName("C11: create invalid date format returns 400 ProblemDetail")
    void c11_create_invalidDate_400(String badDate) throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid\",\"dueDate\":\"" + badDate + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Malformed JSON or invalid field format (dates must be yyyy-MM-dd)")));
    }

    @Test
    @DisplayName("C12: create empty body returns 400")
    void c12_create_emptyBody_400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C13: create wrong Content-Type returns 415")
    void c13_create_wrongContentType_415() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title=hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("C14: list on empty database returns empty JSON array")
    void c14_list_empty() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("C15: list sorts incomplete todos before completed ones")
    void c15_list_incompleteFirst() throws Exception {
        long idA = createTodo("{\"title\":\"A\"}");
        long idB = createTodo("{\"title\":\"B\"}");

        mockMvc.perform(patch("/api/todos/" + idA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is((int) idB)))
                .andExpect(jsonPath("$[1].id", is((int) idA)));
    }

    @Test
    @DisplayName("C16: list sorts by dueDate ascending with nulls last")
    void c16_list_dueDateAscNullsLast() throws Exception {
        long idD = createTodo("{\"title\":\"No date\"}");
        long idE = createTodo("{\"title\":\"Later\",\"dueDate\":\"2026-12-01\"}");
        long idF = createTodo("{\"title\":\"Earlier\",\"dueDate\":\"2026-11-01\"}");

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is((int) idF)))
                .andExpect(jsonPath("$[1].id", is((int) idE)))
                .andExpect(jsonPath("$[2].id", is((int) idD)));
    }

    @Test
    @DisplayName("C17: list tie-breaks identical dates with newest first (id DESC)")
    void c17_list_tieBreakNewestFirst() throws Exception {
        long idG = createTodo("{\"title\":\"First\"}");
        long idH = createTodo("{\"title\":\"Second\"}");

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is((int) idH)))
                .andExpect(jsonPath("$[1].id", is((int) idG)));
    }

    @Test
    @DisplayName("C18: list full sorting rule - incomplete precedes completed even with later due date")
    void c18_list_fullRule() throws Exception {
        long idCompletedEarly = createTodo("{\"title\":\"Done early\",\"dueDate\":\"2026-01-01\"}");
        long idIncompleteNoDate = createTodo("{\"title\":\"Incomplete no date\"}");

        mockMvc.perform(patch("/api/todos/" + idCompletedEarly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is((int) idIncompleteNoDate)))
                .andExpect(jsonPath("$[1].id", is((int) idCompletedEarly)));
    }

    @Test
    @DisplayName("C19: getById returns 200 with matching fields")
    void c19_getById_ok() throws Exception {
        long id = createTodo("{\"title\":\"Read book\",\"description\":\"Chapter 1\",\"dueDate\":\"2026-11-15\"}");

        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.title", is("Read book")))
                .andExpect(jsonPath("$.description", is("Chapter 1")))
                .andExpect(jsonPath("$.dueDate", is("2026-11-15")));
    }

    @Test
    @DisplayName("C20: getById unknown id returns 404 ProblemDetail")
    void c20_getById_unknown_404() throws Exception {
        mockMvc.perform(get("/api/todos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Todo 999999 not found")));
    }

    @Test
    @DisplayName("C21: getById non-numeric id returns 400 ProblemDetail")
    void c21_getById_nonNumeric_400() throws Exception {
        mockMvc.perform(get("/api/todos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("C22: update replaces title, description, and dueDate")
    void c22_update_replacesFields() throws Exception {
        long id = createTodo("{\"title\":\"Initial\",\"description\":\"Desc\",\"dueDate\":\"2026-10-10\"}");

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated",
                                  "description": null,
                                  "dueDate": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated")))
                .andExpect(jsonPath("$.description", nullValue()))
                .andExpect(jsonPath("$.dueDate", nullValue()));
    }

    @Test
    @DisplayName("C23: update never changes completed status (ignored even if sent)")
    void c23_update_neverChangesCompleted() throws Exception {
        long id = createTodo("{\"title\":\"Test\"}");
        mockMvc.perform(patch("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    @DisplayName("C24: update invalid body returns 400 validation error")
    void c24_update_invalid_400() throws Exception {
        long id = createTodo("{\"title\":\"Valid\"}");

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", is("Title is required")));
    }

    @Test
    @DisplayName("C25: update unknown id returns 404")
    void c25_update_unknown_404() throws Exception {
        mockMvc.perform(put("/api/todos/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("C26: update persists changes across subsequent get")
    void c26_update_persists() throws Exception {
        long id = createTodo("{\"title\":\"Before\"}");

        mockMvc.perform(put("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"After\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("After")));
    }

    @Test
    @DisplayName("C27: complete sets true then false")
    void c27_complete_setTrueThenFalse() throws Exception {
        long id = createTodo("{\"title\":\"Toggle me\"}");

        mockMvc.perform(patch("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(true)));

        mockMvc.perform(patch("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(false)));
    }

    @Test
    @DisplayName("C28: complete is idempotent")
    void c28_complete_idempotent() throws Exception {
        long id = createTodo("{\"title\":\"Idempotent\"}");

        mockMvc.perform(patch("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    @DisplayName("C29: complete missing completed field returns 400 validation error")
    void c29_complete_missingField_400() throws Exception {
        long id = createTodo("{\"title\":\"Missing\"}");

        mockMvc.perform(patch("/api/todos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.completed", is("completed is required")));
    }

    @Test
    @DisplayName("C30: complete unknown id returns 404")
    void c30_complete_unknown_404() throws Exception {
        mockMvc.perform(patch("/api/todos/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("C31: delete returns 204 then subsequent get returns 404")
    void c31_delete_204_thenGet404() throws Exception {
        long id = createTodo("{\"title\":\"To be deleted\"}");

        mockMvc.perform(delete("/api/todos/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C32: delete unknown id returns 404")
    void c32_delete_unknown_404() throws Exception {
        mockMvc.perform(delete("/api/todos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("C33: wrong method on collection endpoint returns 405 Method Not Allowed")
    void c33_wrongMethod_405() throws Exception {
        mockMvc.perform(delete("/api/todos"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
    }

    @Test
    @DisplayName("C34: unknown API path returns 404 ProblemDetail")
    void c34_unknownApiPath_404() throws Exception {
        mockMvc.perform(get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
