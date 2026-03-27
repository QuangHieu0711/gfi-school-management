import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { UserInfoAction } from '.';
import { catchError, map, switchMap, throwError } from 'rxjs';
import { AuthService } from '@service';

@Injectable()
export class UserInfoEffect {
  private readonly actions$ = inject(Actions);

  constructor(private readonly authService: AuthService) {}

  getCurrentUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UserInfoAction.GetCurrentUser),
      switchMap(() => {
        return this.authService.getCurrentUser().pipe(
          map((data) => UserInfoAction.Update({ newState: data })),
          catchError((error) => throwError(() => error))
        );
      })
    )
  );
}
