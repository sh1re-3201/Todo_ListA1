class ApiError extends Error {
    constructor(status, message, fieldErrors = {}) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

async function api(path, { method = 'GET', body = null } = {}) {
    const options = {
        method: method.toUpperCase(),
        headers: {
            'Accept': 'application/json'
        }
    };

    if (body !== null) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(path, options);
    } catch (err) {
        throw new ApiError(0, "Can't reach the server");
    }

    if (!response.ok) {
        let errorData = null;
        try {
            errorData = await response.json();
        } catch (_) {
            // response was not JSON
        }

        const message = errorData?.detail || errorData?.title || 'Request failed';
        const fieldErrors = errorData?.errors || {};
        throw new ApiError(response.status, message, fieldErrors);
    }

    if (response.status === 204) {
        return null;
    }

    return await response.json();
}

const listTodos = () => api('/api/todos');
const createTodo = (data) => api('/api/todos', { method: 'POST', body: data });
const updateTodo = (id, data) => api(`/api/todos/${id}`, { method: 'PUT', body: data });
const setCompleted = (id, value) => api(`/api/todos/${id}`, { method: 'PATCH', body: { completed: value } });
const deleteTodo = (id) => api(`/api/todos/${id}`, { method: 'DELETE' });

let todos = [];
let editingId = null;

const addForm = document.getElementById('add-form');
const addTitle = document.getElementById('add-title');
const addTitleError = document.getElementById('add-title-error');
const addDesc = document.getElementById('add-description');
const addDescError = document.getElementById('add-description-error');
const addDue = document.getElementById('add-dueDate');
const addDueError = document.getElementById('add-dueDate-error');

const todoList = document.getElementById('todo-list');
const countSpan = document.getElementById('count');
const loadingP = document.getElementById('loading');
const emptyP = document.getElementById('empty');
const statusDiv = document.getElementById('status');
const errorBanner = document.getElementById('error-banner');

const itemTemplate = document.getElementById('todo-item-template');
const editTemplate = document.getElementById('todo-edit-template');

const dueFormatter = new Intl.DateTimeFormat(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric'
});

function todayLocalISO() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

function parseLocalDate(iso) {
    if (!iso) return null;
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
}

function formatDue(iso) {
    const date = parseLocalDate(iso);
    return date ? dueFormatter.format(date) : '';
}

function dueStatus(todo) {
    if (todo.completed || !todo.dueDate) return null;
    const today = todayLocalISO();
    if (todo.dueDate < today) return 'overdue';
    if (todo.dueDate === today) return 'today';
    return null;
}

function announce(msg) {
    statusDiv.textContent = msg;
}

function showError(msg) {
    errorBanner.textContent = msg;
    errorBanner.hidden = false;
}

function clearError() {
    errorBanner.textContent = '';
    errorBanner.hidden = true;
}

function validate(data) {
    const errors = {};
    if (!data.title || data.title.trim() === '') {
        errors.title = 'Title is required';
    } else if (data.title.length > 200) {
        errors.title = 'Title must be at most 200 characters';
    }
    if (data.description && data.description.length > 1000) {
        errors.description = 'Description must be at most 1000 characters';
    }
    return errors;
}

function showFieldErrors(form, errors) {
    const titleErr = form.querySelector('.field-error[id$="title-error"], .edit-title-error');
    const descErr = form.querySelector('.field-error[id$="description-error"], .edit-desc-error');
    const dueErr = form.querySelector('.field-error[id$="dueDate-error"], .edit-due-error');

    const titleInput = form.querySelector('input[name="title"]');
    const descInput = form.querySelector('textarea[name="description"]');
    const dueInput = form.querySelector('input[name="dueDate"]');

    if (errors.title && titleErr && titleInput) {
        titleErr.textContent = errors.title;
        titleErr.hidden = false;
        titleInput.setAttribute('aria-invalid', 'true');
    }
    if (errors.description && descErr && descInput) {
        descErr.textContent = errors.description;
        descErr.hidden = false;
        descInput.setAttribute('aria-invalid', 'true');
    }
    if (errors.dueDate && dueErr && dueInput) {
        dueErr.textContent = errors.dueDate;
        dueErr.hidden = false;
        dueInput.setAttribute('aria-invalid', 'true');
    }

    if (errors.title && titleInput) {
        titleInput.focus();
    } else if (errors.description && descInput) {
        descInput.focus();
    } else if (errors.dueDate && dueInput) {
        dueInput.focus();
    }
}

function clearFieldErrors(form) {
    const errs = form.querySelectorAll('.field-error');
    for (const el of errs) {
        el.textContent = '';
        el.hidden = true;
    }
    const inputs = form.querySelectorAll('input, textarea');
    for (const el of inputs) {
        el.removeAttribute('aria-invalid');
    }
}

async function withBusy(button, fn) {
    if (button.disabled) return;
    button.disabled = true;
    try {
        await fn();
    } finally {
        button.disabled = false;
    }
}

function readForm(form) {
    const titleVal = form.querySelector('input[name="title"]').value;
    const descVal = form.querySelector('textarea[name="description"]').value;
    const dueVal = form.querySelector('input[name="dueDate"]').value;

    const trimmedTitle = titleVal ? titleVal.trim() : '';
    const trimmedDesc = descVal && descVal.trim() !== '' ? descVal.trim() : null;
    const due = dueVal && dueVal.trim() !== '' ? dueVal.trim() : null;

    return {
        title: trimmedTitle,
        description: trimmedDesc,
        dueDate: due
    };
}

function renderItem(todo) {
    const clone = itemTemplate.content.cloneNode(true);
    const li = clone.querySelector('.todo-item');
    li.dataset.id = String(todo.id);
    if (todo.completed) {
        li.classList.add('is-done');
    }

    const checkbox = li.querySelector('.todo-toggle');
    checkbox.checked = todo.completed;
    checkbox.setAttribute('aria-label', `Mark '${todo.title}' ${todo.completed ? 'incomplete' : 'complete'}`);

    const toggleLabel = li.querySelector('.toggle-label');
    toggleLabel.textContent = `Mark '${todo.title}' ${todo.completed ? 'incomplete' : 'complete'}`;

    const titleEl = li.querySelector('.todo-title');
    titleEl.textContent = todo.title;

    const descEl = li.querySelector('.todo-desc');
    if (todo.description) {
        descEl.textContent = todo.description;
        descEl.hidden = false;
    } else {
        descEl.hidden = true;
    }

    const metaEl = li.querySelector('.todo-meta');
    const badgeEl = li.querySelector('.badge');
    const dueEl = li.querySelector('.todo-due');

    if (todo.dueDate) {
        metaEl.hidden = false;
        dueEl.textContent = formatDue(todo.dueDate);
        dueEl.setAttribute('datetime', todo.dueDate);

        const status = dueStatus(todo);
        if (status === 'overdue') {
            badgeEl.textContent = 'Overdue';
            badgeEl.className = 'badge badge-overdue';
            badgeEl.hidden = false;
        } else if (status === 'today') {
            badgeEl.textContent = 'Today';
            badgeEl.className = 'badge badge-today';
            badgeEl.hidden = false;
        } else {
            badgeEl.hidden = true;
        }
    } else {
        metaEl.hidden = true;
    }

    const editBtn = li.querySelector('[data-action="edit"]');
    editBtn.setAttribute('aria-label', `Edit '${todo.title}'`);

    const deleteBtn = li.querySelector('[data-action="delete"]');
    deleteBtn.setAttribute('aria-label', `Delete '${todo.title}'`);

    return li;
}

function renderEditForm(todo) {
    const clone = editTemplate.content.cloneNode(true);
    const form = clone.querySelector('.todo-edit');
    const li = document.createElement('li');
    li.className = 'todo-item is-editing';
    li.dataset.id = String(todo.id);

    const titleInput = form.querySelector('.edit-title-input');
    titleInput.id = `edit-${todo.id}-title`;
    titleInput.value = todo.title;
    form.querySelector('.edit-title-label').setAttribute('for', titleInput.id);

    const descInput = form.querySelector('.edit-desc-input');
    descInput.id = `edit-${todo.id}-desc`;
    descInput.value = todo.description || '';
    form.querySelector('.edit-desc-label').setAttribute('for', descInput.id);

    const dueInput = form.querySelector('.edit-due-input');
    dueInput.id = `edit-${todo.id}-due`;
    dueInput.value = todo.dueDate || '';
    form.querySelector('.edit-due-label').setAttribute('for', dueInput.id);

    li.appendChild(form);
    return li;
}

function restoreFocus(focusId, selector) {
    if (!focusId) return;
    const target = document.querySelector(`li[data-id="${focusId}"] ${selector || ''}`);
    if (target) {
        target.focus();
        return;
    }
    // If target deleted or gone, focus neighbouring element or heading
    const firstCheckbox = todoList.querySelector('.todo-toggle');
    if (firstCheckbox) {
        firstCheckbox.focus();
    } else {
        document.getElementById('list-heading').focus();
    }
}

function render() {
    todoList.replaceChildren();

    loadingP.hidden = true;
    if (todos.length === 0) {
        emptyP.hidden = false;
        countSpan.textContent = '';
    } else {
        emptyP.hidden = true;
        const activeCount = todos.filter(t => !t.completed).length;
        countSpan.textContent = `(${activeCount} active)`;

        for (const t of todos) {
            if (t.id === editingId) {
                todoList.appendChild(renderEditForm(t));
            } else {
                todoList.appendChild(renderItem(t));
            }
        }
    }
    todoList.setAttribute('aria-busy', 'false');
}

async function loadTodos({ focusId = null, focusSelector = null } = {}) {
    todoList.setAttribute('aria-busy', 'true');
    clearError();
    try {
        todos = await listTodos();
        render();
        if (focusId) {
            restoreFocus(focusId, focusSelector);
        }
    } catch (err) {
        showError(err.message || 'Failed to load to-dos');
        loadingP.hidden = true;
        todoList.setAttribute('aria-busy', 'false');
    }
}

async function onAddSubmit(e) {
    e.preventDefault();
    clearError();
    clearFieldErrors(addForm);

    const data = readForm(addForm);
    const errors = validate(data);
    if (Object.keys(errors).length > 0) {
        showFieldErrors(addForm, errors);
        return;
    }

    const submitBtn = addForm.querySelector('button[type="submit"]');
    await withBusy(submitBtn, async () => {
        try {
            const created = await createTodo(data);
            addForm.reset();
            announce('Added');
            await loadTodos({ focusId: created.id, focusSelector: '.todo-toggle' });
            addTitle.focus();
        } catch (err) {
            if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
                showFieldErrors(addForm, err.fieldErrors);
            } else {
                showError(err.message);
            }
        }
    });
}

function startEdit(id) {
    editingId = Number(id);
    render();
    const titleInput = document.querySelector(`li[data-id="${id}"] .edit-title-input`);
    if (titleInput) {
        titleInput.focus();
        titleInput.select();
    }
}

function cancelEdit(id) {
    editingId = null;
    render();
    restoreFocus(id, '[data-action="edit"]');
}

async function onEditSubmit(form, id) {
    clearError();
    clearFieldErrors(form);

    const data = readForm(form);
    const errors = validate(data);
    if (Object.keys(errors).length > 0) {
        showFieldErrors(form, errors);
        return;
    }

    const submitBtn = form.querySelector('button[type="submit"]');
    await withBusy(submitBtn, async () => {
        try {
            await updateTodo(id, data);
            editingId = null;
            announce('Saved');
            await loadTodos({ focusId: id, focusSelector: '[data-action="edit"]' });
        } catch (err) {
            if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
                showFieldErrors(form, err.fieldErrors);
            } else {
                showError(err.message);
            }
        }
    });
}

async function onListClick(e) {
    const actionBtn = e.target.closest('[data-action]');
    if (!actionBtn) return;

    const li = actionBtn.closest('li');
    const id = li?.dataset.id;
    if (!id) return;

    const action = actionBtn.dataset.action;

    if (action === 'edit') {
        startEdit(id);
    } else if (action === 'cancel') {
        cancelEdit(id);
    } else if (action === 'delete') {
        const todo = todos.find(t => String(t.id) === String(id));
        const title = todo ? todo.title : 'this task';
        if (!window.confirm(`Delete '${title}'?`)) {
            return;
        }

        await withBusy(actionBtn, async () => {
            try {
                await deleteTodo(id);
                announce('Deleted');
                await loadTodos();
            } catch (err) {
                showError(err.message);
            }
        });
    }
}

async function onListChange(e) {
    if (!e.target.classList.contains('todo-toggle')) return;

    const checkbox = e.target;
    const li = checkbox.closest('li');
    const id = li?.dataset.id;
    if (!id) return;

    const isChecked = checkbox.checked;

    await withBusy(checkbox, async () => {
        try {
            await setCompleted(id, isChecked);
            announce(isChecked ? 'Marked complete' : 'Marked incomplete');
            await loadTodos({ focusId: id, focusSelector: '.todo-toggle' });
        } catch (err) {
            checkbox.checked = !isChecked; // revert
            showError(err.message);
        }
    });
}

async function onListSubmit(e) {
    if (!e.target.classList.contains('todo-edit')) return;
    e.preventDefault();

    const form = e.target;
    const li = form.closest('li');
    const id = li?.dataset.id;
    if (!id) return;

    await onEditSubmit(form, id);
}

function onListKeydown(e) {
    const li = e.target.closest('li');
    if (!li) return;
    const id = li.dataset.id;

    if (e.key === 'Escape' && li.classList.contains('is-editing')) {
        cancelEdit(id);
    } else if (e.key === 'Enter' && e.target.tagName === 'TEXTAREA' && (e.ctrlKey || e.metaKey)) {
        const form = li.querySelector('.todo-edit');
        if (form) {
            e.preventDefault();
            onEditSubmit(form, id);
        }
    }
}

function init() {
    addForm.addEventListener('submit', onAddSubmit);
    todoList.addEventListener('click', onListClick);
    todoList.addEventListener('change', onListChange);
    todoList.addEventListener('submit', onListSubmit);
    todoList.addEventListener('keydown', onListKeydown);

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            loadTodos();
        }
    });

    loadTodos();
}

window.addEventListener('DOMContentLoaded', init);
