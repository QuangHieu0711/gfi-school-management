import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { UserInfoAction } from '.';
import { catchError, filter, map, switchMap, throwError } from 'rxjs';
import { AuthService } from '@service';
import { ICurrentUser } from '@model/auth.model';

@Injectable()
export class UserInfoEffect {
  private readonly actions$ = inject(Actions);

  constructor(private readonly authService: AuthService) {}

  getCurrentUser$ = createEffect(() =>
    this.actions$.pipe(
      ofType(UserInfoAction.GetCurrentUser),
      switchMap(() => {
        return this.authService.getCurrentUser().pipe(
          filter((data) => !!data),
          map((data) => {
            const currentUserData: ICurrentUser = {
              id: data!.id,
              username: data!.username,
              fullName: data!.fullName,
              email: data!.email,
              phone: data!.phone,
              status: data!.status,
              role: data!.role,
              unit: data!.unit,
              permissions: data!.permissions,
              rememberMe: data!.rememberMe,
            };
            return UserInfoAction.Update({ newState: currentUserData });
          }),
          catchError((error) => throwError(() => error))
        );
      })
    )
  );
}
