// A deliberately small frontend: state lives in this tab only, not localStorage.
const state = { token: null, user: null, movies: [], shows: [], screens: [], theatres: [],
  selectedShow: null, availableSeats: [], selectedSeats: new Set(), bookings: [], payments: new Map(),
  adminBookingPage: 0, adminBookingPages: 0, adminBookingQuery: '', adminBookingStatus: '' };
const $ = (selector) => document.querySelector(selector);

function element(tag, className, label) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (label !== undefined) node.textContent = label;
  return node;
}

function notice(message, error = false) {
  const box = $('#notice');
  box.textContent = message;
  box.classList.toggle('error', error);
  box.setAttribute('role', error ? 'alert' : 'status');
  box.classList.remove('hidden');
  if (error) box.focus({ preventScroll: true });
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setBusy(button, busy, label) {
  if (!button) return;
  if (busy) {
    button.dataset.originalLabel = button.textContent;
    button.textContent = label;
    button.disabled = true;
  } else {
    button.textContent = button.dataset.originalLabel || button.textContent;
    button.disabled = false;
  }
}

function formMessage(form, message, error = false) {
  let line = form.querySelector('.form-message');
  if (!line) {
    line = element('p', 'form-message');
    line.setAttribute('role', 'alert');
    form.append(line);
  }
  line.textContent = message;
  line.classList.toggle('error', error);
  line.classList.toggle('hidden', !message);
}

async function api(path, options = {}) {
  const headers = { ...options.headers };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { ...options, headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body) });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && state.token && !path.startsWith('/api/auth/')) {
      logout(false);
      throw new Error('Your session has expired. Please sign in again.');
    }
    const detail = data?.validationErrors ? Object.entries(data.validationErrors)
      .map(([field, message]) => `${field}: ${message}`).join('; ') : '';
    throw new Error(detail || data?.message || `Request failed (${response.status})`);
  }
  return data;
}

function showView(name) {
  if (name === 'admin' && state.user?.role !== 'ADMIN') return;
  document.querySelectorAll('.view').forEach(view => view.classList.toggle('hidden', view.id !== `${name}-view`));
  document.querySelectorAll('[data-view]').forEach(button => button.classList.toggle('active', button.dataset.view === name));
  if (name === 'bookings') loadBookings();
  if (name === 'admin') { renderAdminIds(); searchAdminBookings(0); }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function updateAccount() {
  $('#account-name').textContent = state.user ? state.user.name : 'Guest';
  $('#auth-button').textContent = state.user ? 'Sign out' : 'Sign in';
  $('#admin-nav').classList.toggle('hidden', state.user?.role !== 'ADMIN');
}

function openAuth() { formMessage($('#auth-form'), ''); $('#auth-dialog').showModal(); }
function logout(showNotice = true) {
  state.token = null;
  state.user = null;
  state.bookings = [];
  state.payments.clear();
  updateAccount();
  showView('browse');
  if (showNotice) notice('Signed out. Your login was kept only in this browser tab.');
}

let registering = false;
function setAuthMode(register) {
  registering = register;
  $('#auth-title').textContent = register ? 'Create account' : 'Sign in';
  $('#auth-submit').textContent = register ? 'Create account' : 'Sign in';
  $('#auth-switch').textContent = register ? 'Already have an account? Sign in' : 'New here? Create an account';
  $('#name-field').classList.toggle('hidden', !register);
  $('#auth-form [name=name]').required = register;
  $('#auth-form [name=password]').autocomplete = register ? 'new-password' : 'current-password';
  formMessage($('#auth-form'), '');
}

async function handleAuth(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const button = $('#auth-submit');
  const email = form.elements.namedItem('email').value.trim();
  const password = form.elements.namedItem('password').value;
  formMessage(form, '');
  setBusy(button, true, registering ? 'Creating account…' : 'Signing in…');
  try {
    if (registering) {
      await api('/api/auth/register', { method: 'POST', body: {
        name: form.elements.namedItem('name').value.trim(), email, password } });
    }
    const result = await api('/api/auth/login', { method: 'POST', body: { email, password } });
    state.token = result.accessToken;
    state.user = result.user;
    form.reset();
    $('#auth-dialog').close();
    updateAccount();
    notice(`Welcome, ${state.user.name}.`);
    if (state.user.role === 'ADMIN') showView('admin');
    else loadBookings();
  } catch (error) { formMessage(form, error.message, true); }
  finally { setBusy(button, false); }
}

const dateLabel = (date) => new Date(`${date}T12:00:00`).toLocaleDateString(undefined,
  { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
const amountLabel = (amount) => Number(amount).toFixed(2);
const movieFor = (id) => state.movies.find(movie => movie.id === id);
const screenFor = (id) => state.screens.find(screen => screen.id === id);
const theatreFor = (id) => state.theatres.find(theatre => theatre.id === id);

async function loadCatalog() {
  const refresh = $('#refresh-shows');
  setBusy(refresh, true, 'Refreshing…');
  try {
    const [movies, shows, screens, theatres] = await Promise.all([
      api('/api/movies?size=100'), api('/api/shows?size=100'),
      api('/api/screens?size=100'), api('/api/theatres?size=100')]);
    state.movies = movies.content;
    state.shows = shows.content;
    state.screens = screens.content;
    state.theatres = theatres.content;
    renderShows();
    renderTheatreFilter();
    renderAdminIds();
    renderShowFormOptions();
  } catch (error) {
    $('#shows-list').replaceChildren(element('div', 'empty-state', 'Could not load shows. Check the server and use Refresh to try again.'));
    notice(error.message, true);
  } finally { setBusy(refresh, false); }
}

function renderTheatreFilter() {
  const select = $('#theatre-filter');
  const selected = select.value;
  select.replaceChildren(new Option('All theatres', ''));
  for (const theatre of state.theatres) select.add(new Option(theatre.name, theatre.id));
  if ([...select.options].some(option => option.value === selected)) select.value = selected;
}

function renderShowFormOptions() {
  const movieSelect = $('#show-form [name=movieId]');
  const screenSelect = $('#show-form [name=screenId]');
  const movieValue = movieSelect.value;
  const screenValue = screenSelect.value;
  movieSelect.replaceChildren(new Option('Choose a movie', ''));
  for (const movie of state.movies) movieSelect.add(new Option(`${movie.title} (#${movie.id})`, movie.id));
  screenSelect.replaceChildren(new Option('Choose a screen', ''));
  for (const screen of state.screens) {
    const theatre = theatreFor(screen.theatreId);
    screenSelect.add(new Option(`${theatre?.name || `Theatre #${screen.theatreId}`} · ${screen.name} (#${screen.id})`, screen.id));
  }
  if ([...movieSelect.options].some(option => option.value === movieValue)) movieSelect.value = movieValue;
  if ([...screenSelect.options].some(option => option.value === screenValue)) screenSelect.value = screenValue;
  movieSelect.options[0].textContent = state.movies.length ? 'Choose a movie' : 'Add a movie first';
  screenSelect.options[0].textContent = state.screens.length ? 'Choose a screen' : 'Add a screen first';
}

function renderShows() {
  const list = $('#shows-list');
  list.replaceChildren();
  const upcoming = state.shows.filter(show => new Date(`${show.showDate}T${show.startTime}`) > new Date())
    .sort((a, b) => `${a.showDate}${a.startTime}`.localeCompare(`${b.showDate}${b.startTime}`));
  if (!upcoming.length) {
    $('#show-count').textContent = '0 upcoming shows';
    list.append(element('div', 'empty-state', 'No upcoming shows yet. An admin can add a movie, screen, show, and seats.'));
    return;
  }
  const query = $('#show-search').value.trim().toLocaleLowerCase();
  const theatreId = $('#theatre-filter').value;
  const date = $('#date-filter').value;
  const filtered = upcoming.filter(show => {
    const movie = movieFor(show.movieId);
    const screen = screenFor(show.screenId);
    const theatre = theatreFor(screen?.theatreId);
    const text = `${movie?.title || ''} ${movie?.genre || ''} ${theatre?.name || ''} ${screen?.name || ''}`.toLocaleLowerCase();
    return (!query || text.includes(query))
      && (!theatreId || String(screen?.theatreId) === theatreId)
      && (!date || show.showDate === date);
  });
  $('#show-count').textContent = `${filtered.length} of ${upcoming.length} upcoming show${upcoming.length === 1 ? '' : 's'}`;
  if (!filtered.length) {
    list.append(element('div', 'empty-state', 'No shows match these filters. Try a different search or clear the filters.'));
    return;
  }
  for (const show of filtered) {
    const movie = movieFor(show.movieId);
    const screen = screenFor(show.screenId);
    const theatre = theatreFor(screen?.theatreId);
    const card = element('article', 'show-card');
    const poster = element('div', 'show-poster');
    poster.append(element('span', '', movie?.title || `Movie #${show.movieId}`));
    const body = element('div', 'show-body');
    body.append(element('div', 'show-meta', `${dateLabel(show.showDate)} · ${show.startTime.slice(0, 5)}`));
    body.append(element('h3', '', movie?.title || `Movie #${show.movieId}`));
    body.append(element('div', 'show-details', `${movie?.genre || 'Film'} · ${movie?.duration || '—'} min\n${theatre?.name || 'Theatre'} / ${screen?.name || `Screen #${show.screenId}`}`));
    const button = element('button', 'button button-outline', 'Choose seats →');
    button.addEventListener('click', () => loadSeats(show));
    body.append(button);
    card.append(poster, body);
    list.append(card);
  }
}

async function loadSeats(show) {
  state.selectedShow = show;
  state.availableSeats = [];
  state.selectedSeats.clear();
  $('#seat-title').textContent = movieFor(show.movieId)?.title || `Show #${show.id}`;
  $('#seat-subtitle').textContent = `${dateLabel(show.showDate)} at ${show.startTime.slice(0, 5)} · Show #${show.id}`;
  $('#seat-panel').classList.remove('hidden');
  $('#seat-list').replaceChildren(element('p', 'muted', 'Checking available seats…'));
  $('#seat-summary').textContent = 'Choose a seat after availability loads';
  $('#book-button').disabled = true;
  try {
    const result = await api(`/api/shows/${show.id}/available-seats?size=100`);
    state.availableSeats = result.content;
    renderSeats();
    $('#seat-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (error) {
    $('#seat-list').replaceChildren(element('p', 'muted', 'Seats could not be loaded. Close this panel and try again.'));
    notice(error.message, true);
  }
}

function renderSeats() {
  const list = $('#seat-list');
  list.replaceChildren();
  if (!state.availableSeats.length) list.append(element('p', 'muted', 'No seats are available for this show.'));
  for (const seat of state.availableSeats) {
    const button = element('button', 'seat-choice', `${seat.rowNumber}${seat.seatNumber}`);
    button.type = 'button';
    button.setAttribute('aria-pressed', String(state.selectedSeats.has(seat.id)));
    button.setAttribute('aria-label', `Seat ${seat.rowNumber}${seat.seatNumber}, price ${amountLabel(seat.price)}`);
    if (state.selectedSeats.has(seat.id)) button.classList.add('selected');
    button.append(element('small', '', amountLabel(seat.price)));
    button.addEventListener('click', () => {
      if (state.selectedSeats.has(seat.id)) state.selectedSeats.delete(seat.id);
      else state.selectedSeats.add(seat.id);
      button.classList.toggle('selected', state.selectedSeats.has(seat.id));
      button.setAttribute('aria-pressed', String(state.selectedSeats.has(seat.id)));
      updateSeatSummary();
    });
    list.append(button);
  }
  updateSeatSummary();
}

function updateSeatSummary() {
  const chosen = state.availableSeats.filter(seat => state.selectedSeats.has(seat.id));
  const total = chosen.reduce((sum, seat) => sum + Number(seat.price), 0);
  $('#seat-summary').textContent = chosen.length ? `${chosen.length} seat${chosen.length === 1 ? '' : 's'} · Total ${amountLabel(total)}` : 'Select a seat to continue';
  $('#book-button').disabled = chosen.length === 0;
}

async function bookSeats() {
  if (!state.user) { openAuth(); return; }
  const button = $('#book-button');
  setBusy(button, true, 'Reserving…');
  try {
    const booking = await api('/api/bookings', { method: 'POST', body: {
      showId: state.selectedShow.id, seatIds: [...state.selectedSeats] } });
    notice(`Booking #${booking.id} reserved. You can record a cash payment from My bookings.`);
    await loadSeats(state.selectedShow);
    showView('bookings');
  } catch (error) {
    notice(error.message, true);
    await loadSeats(state.selectedShow);
  } finally {
    setBusy(button, false);
    button.disabled = state.selectedSeats.size === 0;
  }
}

async function loadBookings() {
  if (!state.user) {
    $('#bookings-list').replaceChildren(element('div', 'empty-state', 'Sign in to view your bookings.'));
    return;
  }
  $('#bookings-list').replaceChildren(element('div', 'empty-state', 'Loading your bookings…'));
  try {
    const result = await api('/api/bookings?size=100&sort=bookingDate,desc');
    state.bookings = result.content;
    await Promise.all(state.bookings.filter(booking => booking.paymentId).map(async booking => {
      try { state.payments.set(booking.id, await api(`/api/payments/${booking.paymentId}`)); }
      catch { /* A missing payment should not hide the booking. */ }
    }));
    renderBookings();
  } catch (error) {
    $('#bookings-list').replaceChildren(element('div', 'empty-state', 'Bookings could not be loaded. Select My bookings to retry.'));
    notice(error.message, true);
  }
}

async function searchAdminBookings(page = 0) {
  if (state.user?.role !== 'ADMIN') return;
  const results = $('#admin-booking-results');
  results.replaceChildren(element('p', 'muted', 'Loading bookings…'));
  const params = new URLSearchParams({ page: String(page), size: '20', sort: 'bookingDate,desc' });
  if (state.adminBookingQuery) params.set('query', state.adminBookingQuery);
  if (state.adminBookingStatus) params.set('status', state.adminBookingStatus);
  try {
    const data = await api(`/api/bookings/admin/search?${params}`);
    state.adminBookingPage = data.page;
    state.adminBookingPages = data.totalPages;
    results.replaceChildren();
    if (!data.content.length) {
      results.append(element('p', 'muted', 'No bookings match your search.'));
    } else {
      for (const item of data.content) {
        const booking = item.booking;
        const card = element('article', 'admin-booking-result');
        const heading = element('div', 'admin-booking-result-heading');
        heading.append(element('strong', '', `${item.customerName} · ${booking.bookingReference}`));
        heading.append(element('span', `badge ${booking.status.toLowerCase()}`, booking.status));
        card.append(heading);
        card.append(element('p', '', `${item.customerEmail} · Booking #${booking.id}`));
        card.append(element('p', '', `Show #${booking.showId} · ${booking.seats.length} seat${booking.seats.length === 1 ? '' : 's'} · ${amountLabel(booking.totalAmount)} · ${dateLabel(booking.bookingDate.slice(0, 10))}`));
        if (booking.paymentId) card.append(element('p', '', `Payment #${booking.paymentId}`));
        results.append(card);
      }
    }
    const pagination = $('#admin-booking-pagination');
    pagination.classList.toggle('hidden', data.totalPages < 2);
    $('#admin-booking-page').textContent = `Page ${data.page + 1} of ${data.totalPages || 1} · ${data.totalElements} bookings`;
    $('#admin-booking-prev').disabled = data.page === 0;
    $('#admin-booking-next').disabled = data.page + 1 >= data.totalPages;
  } catch (error) {
    results.replaceChildren(element('p', 'form-message error', error.message));
  }
}

function submitAdminBookingSearch(event) {
  event.preventDefault();
  const form = event.currentTarget;
  state.adminBookingQuery = form.elements.namedItem('query').value.trim();
  state.adminBookingStatus = form.elements.namedItem('status').value;
  searchAdminBookings(0);
}

function renderBookings() {
  const list = $('#bookings-list');
  list.replaceChildren();
  if (!state.bookings.length) {
    list.append(element('div', 'empty-state', 'No bookings yet. Explore the upcoming shows to reserve your first seat.'));
    return;
  }
  for (const booking of state.bookings) {
    const show = state.shows.find(item => item.id === booking.showId);
    const movie = movieFor(show?.movieId);
    const payment = state.payments.get(booking.id);
    const card = element('article', 'booking-card');
    const top = element('div', 'booking-top');
    const title = element('div');
    title.append(element('h3', '', movie?.title || `Booking #${booking.id}`));
    title.append(element('p', '', show ? `${dateLabel(show.showDate)} · ${show.startTime.slice(0, 5)} · Show #${show.id}` : `Show #${booking.showId}`));
    top.append(title, element('span', `badge ${booking.status.toLowerCase()}`, booking.status));
    card.append(top);
    card.append(element('p', '', `Reference: ${booking.bookingReference}`));
    card.append(element('p', '', `Total: ${amountLabel(booking.totalAmount)} · Seats: ${booking.seats.map(seat => `${seat.rowNumber}${seat.seatNumber}`).join(', ') || 'Released'}`));
    card.append(element('p', '', `Cash payment: ${payment?.status || (booking.paymentId ? 'Loading…' : 'Not started')}${payment?.id ? ` · ID #${payment.id}` : ''}`));
    if (payment?.confirmedByUserId && payment?.confirmedAt) {
      card.append(element('p', '', `Cash confirmed by admin #${payment.confirmedByUserId} on ${new Date(payment.confirmedAt).toLocaleString()}`));
    }
    if (payment?.refundedByUserId && payment?.refundedAt) {
      card.append(element('p', '', `Cash refunded by admin #${payment.refundedByUserId} on ${new Date(payment.refundedAt).toLocaleString()}`));
    }
    const actions = element('div', 'booking-actions');
    if (booking.status === 'CONFIRMED' && !booking.paymentId && !payment) {
      const pay = element('button', 'button button-primary', 'Record cash payment');
      pay.addEventListener('click', () => createPayment(booking, pay));
      actions.append(pay);
    }
    if (booking.status === 'CONFIRMED' && !['COMPLETED', 'REFUNDED'].includes(payment?.status)) {
      const cancel = element('button', 'button button-outline', 'Cancel booking');
      cancel.addEventListener('click', () => cancelBooking(booking, cancel));
      actions.append(cancel);
    }
    card.append(actions);
    list.append(card);
  }
}

async function createPayment(booking, button) {
  setBusy(button, true, 'Recording…');
  try {
    const payment = await api('/api/payments', { method: 'POST', body: { bookingId: booking.id, method: 'CASH' } });
    state.payments.set(booking.id, payment);
    notice(`Cash payment #${payment.id} is pending. Give this ID to an admin after paying in person.`);
    await loadBookings();
  } catch (error) { notice(error.message, true); }
  finally { setBusy(button, false); }
}

async function cancelBooking(booking, button) {
  if (!window.confirm(`Cancel booking #${booking.id} and release its seats?`)) return;
  setBusy(button, true, 'Cancelling…');
  try {
    await api(`/api/bookings/${booking.id}/cancel`, { method: 'POST' });
    notice(`Booking #${booking.id} cancelled. Its seats are available again.`);
    await loadBookings();
    if (state.selectedShow?.id === booking.showId) await loadSeats(state.selectedShow);
  } catch (error) { notice(error.message, true); }
  finally { setBusy(button, false); }
}

function renderAdminIds() {
  if (state.user?.role !== 'ADMIN') return;
  const box = $('#admin-ids');
  box.replaceChildren();
  for (const [label, values, name] of [
    ['Theatres', state.theatres, 'name'], ['Screens', state.screens, 'name'],
    ['Movies', state.movies, 'title'], ['Shows', state.shows, 'showDate']]) {
    const line = element('div');
    line.append(element('strong', '', `${label}: `));
    line.append(document.createTextNode(values.length ? values.map(value => `#${value.id} ${value[name]}`).join(' · ') : 'None yet'));
    box.append(line);
  }
}

async function confirmCash(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const id = form.elements.namedItem('paymentId').value;
  if (!window.confirm(`Confirm cash was actually received for payment #${id}?`)) return;
  const button = form.querySelector('button[type=submit]');
  formMessage(form, '');
  setBusy(button, true, 'Confirming…');
  try {
    const payment = await api(`/api/payments/${id}/confirm-cash`, { method: 'POST' });
    notice(`Cash payment #${payment.id} completed. Receipt: ${payment.transactionReference}`);
    form.reset();
    formMessage(form, `Payment #${payment.id} completed by admin #${payment.confirmedByUserId} at ${new Date(payment.confirmedAt).toLocaleString()}. Receipt: ${payment.transactionReference}`);
    await loadBookings();
  } catch (error) { formMessage(form, error.message, true); }
  finally { setBusy(button, false); }
}

async function lookUpPayment() {
  const form = $('#confirm-form');
  const field = form.elements.namedItem('paymentId');
  if (!field.reportValidity()) return;
  const button = $('#lookup-payment');
  formMessage(form, '');
  setBusy(button, true, 'Looking up…');
  try {
    const payment = await api(`/api/payments/admin/${field.value}`);
    const audit = payment.confirmedByUserId && payment.confirmedAt
      ? ` Confirmed by admin #${payment.confirmedByUserId} at ${new Date(payment.confirmedAt).toLocaleString()}.`
      : payment.status === 'COMPLETED' ? ' This payment was confirmed before audit tracking was added.' : '';
    const refundAudit = payment.refundedByUserId && payment.refundedAt
      ? ` Refunded by admin #${payment.refundedByUserId} at ${new Date(payment.refundedAt).toLocaleString()}.` : '';
    formMessage(form, `Payment #${payment.id}: ${payment.status}, ${payment.method}, amount ${amountLabel(payment.amount)}.${audit}${refundAudit}`);
  } catch (error) { formMessage(form, error.message, true); }
  finally { setBusy(button, false); }
}

async function refundCash(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const id = form.elements.namedItem('paymentId').value;
  if (!window.confirm(`Confirm cash was actually returned for payment #${id}?`)) return;
  const button = form.querySelector('button[type=submit]');
  formMessage(form, '');
  setBusy(button, true, 'Recording…');
  try {
    const payment = await api(`/api/payments/${id}/refund-cash`, { method: 'POST' });
    notice(`Cash refund for payment #${payment.id} recorded.`);
    form.reset();
    formMessage(form, `Payment #${payment.id} refunded by admin #${payment.refundedByUserId} at ${new Date(payment.refundedAt).toLocaleString()}.`);
    await loadBookings();
  } catch (error) { formMessage(form, error.message, true); }
  finally { setBusy(button, false); }
}

async function addCatalogueItem(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const body = Object.fromEntries(new FormData(form));
  for (const key of ['capacity', 'theatreId', 'duration', 'movieId', 'screenId', 'price']) {
    if (body[key] !== undefined) body[key] = Number(body[key]);
  }
  const button = form.querySelector('button[type=submit], button');
  formMessage(form, '');
  setBusy(button, true, 'Saving…');
  try {
    const result = await api(`/api/${form.dataset.endpoint}`, { method: 'POST', body });
    notice(`${form.dataset.endpoint.slice(0, -1)} #${result.id} added.`);
    form.reset();
    formMessage(form, `Created #${result.id}.`);
    await loadCatalog();
  } catch (error) { formMessage(form, error.message, true); }
  finally { setBusy(button, false); }
}

document.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => showView(button.dataset.view)));
$('#auth-button').addEventListener('click', () => state.user ? logout() : openAuth());
$('#close-auth').addEventListener('click', () => $('#auth-dialog').close());
$('#auth-switch').addEventListener('click', () => setAuthMode(!registering));
$('#auth-form').addEventListener('submit', handleAuth);
$('#explore-button').addEventListener('click', () => $('#showtimes').scrollIntoView({ behavior: 'smooth' }));
$('#refresh-shows').addEventListener('click', loadCatalog);
$('#show-search').addEventListener('input', renderShows);
$('#theatre-filter').addEventListener('change', renderShows);
$('#date-filter').addEventListener('change', renderShows);
$('#clear-show-filters').addEventListener('click', () => {
  $('#show-search').value = '';
  $('#theatre-filter').value = '';
  $('#date-filter').value = '';
  renderShows();
});
$('#admin-booking-search-form').addEventListener('submit', submitAdminBookingSearch);
$('#admin-booking-prev').addEventListener('click', () => searchAdminBookings(state.adminBookingPage - 1));
$('#admin-booking-next').addEventListener('click', () => searchAdminBookings(state.adminBookingPage + 1));
$('#close-seats').addEventListener('click', () => $('#seat-panel').classList.add('hidden'));
$('#book-button').addEventListener('click', bookSeats);
$('#confirm-form').addEventListener('submit', confirmCash);
$('#refund-form').addEventListener('submit', refundCash);
$('#lookup-payment').addEventListener('click', lookUpPayment);
document.querySelectorAll('.admin-form').forEach(form => form.addEventListener('submit', addCatalogueItem));
window.setInterval(() => {
  if (!document.hidden && !$('#browse-view').classList.contains('hidden')) loadCatalog();
}, 30000);
loadCatalog();
